package controllers;
import beans.Offerta;
import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import beans.Asta;
import beans.AstaStatus;
import beans.User;

import connection.ConnectionHandler;
import dao.AsteDao;
import dao.OffertaDao;

@WebServlet("/ChiudiAsta")
public class ChiudiAsta extends HttpServlet {
	private static final long serialVersionUID = 1L;
	private Connection connection = null;

	public ChiudiAsta() {
		super();
	}

	public void init() throws ServletException {
		connection = ConnectionHandler.getConnection(getServletContext());
	}

	protected void doPost(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {

		// If the user is not logged in (not present in session) redirect to the login
		String loginpath = getServletContext().getContextPath() + "/index.html";
		HttpSession session = request.getSession();
		if (session.isNew() || session.getAttribute("user") == null) {
			response.sendRedirect(loginpath);
			return;
		}

		// get and check params
		Integer idAsta = null;
		Integer idVincitoreAsta = null;
		Float prezzoAggiudica = null;
		OffertaDao offertaDao = new OffertaDao(connection);
		Offerta offerta = null;
		User user = (User) session.getAttribute("user");
		AsteDao astaDao = new AsteDao(connection);
		
		try {
			idAsta = Integer.parseInt(request.getParameter("idAsta"));
		} catch (NumberFormatException | NullPointerException e) {
			  e.printStackTrace();
			response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Incorrect param values");
			return;
		}
		
		try {
			offerta = offertaDao.findOffertaVincente(idAsta);
			
		} catch (SQLException  e) {
			 e.printStackTrace();
			response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Incorrect param values");
			return;
		}
		
		try {
			idAsta = offerta.getIdAsta();
			System.out.println("idAsta is"+idAsta);

			idVincitoreAsta = offerta.getIdOfferente();
			prezzoAggiudica = offerta.getAmmontareOfferta();
		} catch (NumberFormatException | NullPointerException e) {
			 e.printStackTrace();
			response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Incorrect param values");
			return;
		}

		try {
			// Check that only the user who created the bid closes it
			Asta asta = astaDao.findAstaById(idAsta);
			
			if (asta == null) {
				response.sendError(HttpServletResponse.SC_PRECONDITION_FAILED, "Asta not found");
				return;
			}
			
			if (asta.getSellerId() != user.getUserId()) {
				System.out.println(asta.getSellerId());
				System.out.println(user.getUserId());
				
				response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Not allowed");
				return;
			}
			astaDao.changeAstaStatus(idAsta,idVincitoreAsta,prezzoAggiudica, AstaStatus.CLOSED);
		} catch (SQLException e) {
			 e.printStackTrace();
			response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Asta not closable");
			return;
		}

		// Return view
		String ctxpath = getServletContext().getContextPath();
		String path = ctxpath + "/GetDettaglioAsta?idAsta=" + idAsta;
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
