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
import beans.User;
import dao.AsteDao;
import connection.ConnectionHandler;

	@WebServlet("/GetVendo")
	public class GetVendo extends HttpServlet {
		private static final long serialVersionUID = 1L;
		private Connection connection = null;
		private TemplateEngine templateEngine;

		public GetVendo() {
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
			List<Asta> dettAstArtcliAprt = null;
			List<Asta> dettAstArtcliCs = null;
			
			HttpSession s = request.getSession();
			long time = request.getSession().getCreationTime();
			Timestamp loginTime = new Timestamp(time);
			user = (User) s.getAttribute("user");
			AsteDao asteDao = new AsteDao(connection);
			

			try {
				dettAstArtcliAprt = asteDao.findAsteEarticoloAperteBySellerId(user.getUserId(),loginTime);
				dettAstArtcliCs = asteDao.findAsteEarticoloChiuseBySellerId(user.getUserId(),loginTime);

			} catch (SQLException e) {
				 throw new ServletException(e);
				//response.sendError(HttpServletResponse.SC_BAD_GATEWAY, "Failure in seller's database extraction");
			}
			String path = "/Vendo.html";
			ServletContext servletContext = getServletContext();
			final WebContext ctx = new WebContext(request, response, servletContext, request.getLocale());
			ctx.setVariable("dettAstArtcliAprt", dettAstArtcliAprt);
			ctx.setVariable("dettAstArtcliCs", dettAstArtcliCs);
			ctx.setVariable("loginTime", loginTime);

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
		

		
		

			