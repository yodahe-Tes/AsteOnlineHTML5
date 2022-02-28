package controllers;

import java.io.IOException;
import java.sql.Timestamp;

import java.sql.Connection;
import java.sql.SQLException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.commons.lang.StringEscapeUtils;

import beans.Asta;
import beans.User;
import dao.AsteDao;
import dao.OffertaDao;
import connection.ConnectionHandler;

@WebServlet("/FaiOfferta")
public class FaiOfferta extends HttpServlet {

	private static final long serialVersionUID = 1L;

	private Connection connection = null;

	public FaiOfferta() {
		super();
	}

	public void init() throws ServletException {
		connection = ConnectionHandler.getConnection(getServletContext());
		
	}


	

	protected void doPost(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		// If the user is not logged in (not present in session) redirect to the login
		HttpSession session = request.getSession();
		if (session.isNew() || session.getAttribute("user") == null) {
			String loginpath = getServletContext().getContextPath() + "/index.html";
			response.sendRedirect(loginpath);
			return;
		}

		// Get and parse all parameters from request
		boolean isBadRequest = false;
		Float ammontareOfferta= null;
		Float offertaPreced = null;
		Float rialzoMinimo = null;
		Timestamp dataOraOfferta = null;
		Integer idAsta = null;
		Integer idOfferente = null;
		User user = (User) session.getAttribute("user");
		AsteDao asteDao = new AsteDao(connection);
		Asta asta = null;
		
		
		
		try {
			ammontareOfferta = Float.parseFloat(request.getParameter("ammontareOfferta"));
			System.out.println("ammontare offerta is: "+ammontareOfferta);
			SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
			dataOraOfferta = new Timestamp(System.currentTimeMillis());;
			System.out.println("dataOra offerta is: "+dataOraOfferta);
			idAsta = Integer.parseInt(request.getParameter("idAsta"));
			System.out.println("id Asta is: "+idAsta);
			idOfferente = user.getUserId();
			System.out.println("id offerente is: "+idOfferente);
			isBadRequest = ammontareOfferta <= 0 ;
			
			
			//|| (ammontareOfferta - max(ammontareOfferta)) < rialzoMinimo ;	
			
		} catch (NumberFormatException | NullPointerException e) {
			isBadRequest = true;
			e.printStackTrace();
		}
		if (isBadRequest) {
			
			response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Inserisci un offerta valida");
			return;
		}
		
		try {
			asta = asteDao.findAstaEarticoloByIdAsta(idAsta);
			offertaPreced = asta.getPrezzoAggiudica();
			rialzoMinimo = asta.getrialzoMinimo();
			System.out.println("ammontare offerta is: "+ammontareOfferta);
			System.out.println("offertaPrec  is: "+offertaPreced);
			System.out.println("rialzo min is: "+rialzoMinimo);
			isBadRequest = (ammontareOfferta - offertaPreced) < rialzoMinimo ;

			if (idOfferente == asta.getSellerId()) {
				response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Non puoi fare offerte sulla tua asta");
				return;
			}

		} catch (SQLException e1) {
			isBadRequest = true;
			e1.printStackTrace();
		}
		System.out.println("offertaPrec  is: "+offertaPreced);
		System.out.println("rialzo min is: "+rialzoMinimo);
		if (isBadRequest) {
			response.sendError(HttpServletResponse.SC_BAD_REQUEST, "offerta minore del rialzo Minimo");
			return;
		}
		
			
		OffertaDao offertaDao = new OffertaDao(connection);
		try {
			offertaDao.creaOfferta(ammontareOfferta, dataOraOfferta, idAsta, idOfferente);
		} catch (SQLException e) {
			e.printStackTrace();
			//response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Not possible to create Offerta");
			return;
		}
		

		// return the user to the right view
		String ctxpath = getServletContext().getContextPath();
		String path = ctxpath + "/GetOfferta?idAsta=" + idAsta;
		response.sendRedirect(path);
	}

	public void destroy() {
		try {
			ConnectionHandler.closeConnection(connection);
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}
}