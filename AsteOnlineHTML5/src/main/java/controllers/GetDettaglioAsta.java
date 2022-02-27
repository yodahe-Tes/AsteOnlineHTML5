package controllers;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.List;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
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
import connection.ConnectionHandler;
import dao.AsteDao;
import dao.OffertaDao;
import dao.UserDao;

@WebServlet("/GetDettaglioAsta")
public class GetDettaglioAsta extends HttpServlet {
	private static final long serialVersionUID = 1L;
	private Connection connection = null;
	private TemplateEngine templateEngine;

	public GetDettaglioAsta() {
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

		// get and check params
		Integer idVincitore = null;
		Integer idAsta = null;
		
		try {
			idAsta = Integer.parseInt(request.getParameter("idAsta"));
		} catch (NumberFormatException | NullPointerException e) {
			  e.printStackTrace();
			response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Incorrect param values");
			return;
		}

		
		UserDao userDao = new UserDao(connection);
		User user = (User) session.getAttribute("user");
		User vincitoreAsta = null;
		AsteDao astaDao = new AsteDao(connection);
		Asta detAsta = null;
		Asta detAstaCs = null;

		OffertaDao offertaDao = new OffertaDao(connection);
		List<Offerta> offerte = null;
		Timestamp currTime = null;

		try {
			detAsta = astaDao.findAstaEarticoloByIdAsta(idAsta);
			detAstaCs = astaDao.findAstaEarticoloCsByIdAsta(idAsta);
			offerte = offertaDao.findTutteOfferteByIdAsta(idAsta);
		    
			if (detAsta == null ) {
				response.sendError(HttpServletResponse.SC_NOT_FOUND, "Resource not found");
				return;
				
			}
			
			if (detAsta.isClosed()) {
				idVincitore = detAstaCs.getVincitoreAsta();
				System.out.println(idVincitore);

				vincitoreAsta = userDao.findUserById(idVincitore);
				System.out.println(vincitoreAsta.getIndirizzo());
			}
			
		} catch (SQLException e) {
			e.printStackTrace();
			response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Not possible to recover asta");
			return;
		}
		currTime = new Timestamp(System.currentTimeMillis());

		// Redirect to Dettaglio asta
		String path = "/DettaglioAsta.html";
		ServletContext servletContext = getServletContext();
		final WebContext ctx = new WebContext(request, response, servletContext, request.getLocale());
		ctx.setVariable("detAsta", detAsta);
		ctx.setVariable("detAstaCs", detAstaCs);
		ctx.setVariable("offerte", offerte);
		ctx.setVariable("currTime", currTime);
		if(detAsta.isClosed()) {
			ctx.setVariable("vincitoreAsta", vincitoreAsta);
			System.out.println(vincitoreAsta.getName());
			System.out.println(vincitoreAsta.getIndirizzo());

		}
		
		
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
