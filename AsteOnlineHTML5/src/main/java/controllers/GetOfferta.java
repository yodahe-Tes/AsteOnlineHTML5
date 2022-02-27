package controllers;

import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.List;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.UnavailableException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.WebContext;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.ServletContextTemplateResolver;

import beans.Asta;
import beans.Offerta;
import beans.User;
import dao.AsteDao;
import dao.OffertaDao;
import connection.ConnectionHandler;

	@WebServlet("/GetOfferta")
	public class GetOfferta extends HttpServlet {
		private static final long serialVersionUID = 1L;
		private Connection connection = null;
		private TemplateEngine templateEngine;

		public GetOfferta() {
			super();
		}

		public void init() throws ServletException {
			ServletContext servletContext = getServletContext();
			ServletContextTemplateResolver templateResolver = new ServletContextTemplateResolver(servletContext);
			templateResolver.setTemplateMode(TemplateMode.HTML);
			this.templateEngine = new TemplateEngine();
			this.templateEngine.setTemplateResolver(templateResolver);
			templateResolver.setSuffix(".html");

			connection = ConnectionHandler.getConnection(getServletContext());
		}

		protected void doGet(HttpServletRequest request, HttpServletResponse response)
				throws ServletException, IOException {
			
			// If the user is not logged in (not present in session) redirect to the login
			String loginpath = getServletContext().getContextPath() + "/index.html";
			HttpSession session = request.getSession();
			if (session.isNew() || session.getAttribute("user") == null) {
				response.sendRedirect(loginpath);
				return;
			}
			
			User user = null;
			Integer idAsta = null;
			Asta asta = null;
			List<Offerta> offerte = null;
			Asta detAsta = null;
			
			HttpSession s = request.getSession();
			user = (User) s.getAttribute("user");
			OffertaDao offertaDao = new OffertaDao(connection);
			AsteDao detAstaDao = new AsteDao(connection);
			Timestamp currTime = null;

			
			try {
				idAsta = Integer.parseInt(request.getParameter("idAsta"));
				System.out.println("idAsta :"+ idAsta);
			} catch (NumberFormatException | NullPointerException e) {
				  e.printStackTrace();
				response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Incorrect param values");
				return;
			}
			try { 
				detAsta = detAstaDao.findAstaEarticoloByIdAsta(idAsta);
				
				if (detAsta == null) {
					response.sendError(HttpServletResponse.SC_NOT_FOUND, "Resource not found");
					return;
				}
			
			offerte = offertaDao.findOfferteByIdAsta(idAsta);
				
			} catch (SQLException e) {
				 throw new ServletException(e);
				//response.sendError(HttpServletResponse.SC_BAD_GATEWAY, "Failure in asta's database extraction");
			}
			currTime = new Timestamp(System.currentTimeMillis());
			String path = "/Offerta.html";
			ServletContext servletContext = getServletContext();
			final WebContext ctx = new WebContext(request, response, servletContext, request.getLocale());
			ctx.setVariable("offerte", offerte);
			ctx.setVariable("detAsta", detAsta);
			ctx.setVariable("currTime", currTime);

			
			templateEngine.process(path, ctx, response.getWriter());
		}

		public void destroy() {
			try {
				ConnectionHandler.closeConnection(connection);
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}
		
	}
		
		