package com.codecool.web;

import com.codecool.web.DAO.userDAO;
import com.codecool.web.Model.User;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import org.jtwig.JtwigModel;
import org.jtwig.JtwigTemplate;

import java.io.*;
import java.net.HttpCookie;
import java.net.URLDecoder;
import java.util.*;

public class Cookie implements HttpHandler {
    Map<String, String> sessionIDs = new HashMap<>();

    @Override
    public void handle(HttpExchange httpExchange) throws IOException {
        String method = httpExchange.getRequestMethod();
        String response = "";
        HttpCookie cookie = null;

        cookie = getCookie(httpExchange, cookie);

        userDAO userDAO = new userDAO();
        // Send a form if it wasn't submitted yet.
        if(method.equals("GET")){

            if(isSessionValid(sessionIDs, cookie)) {
                String login = sessionIDs.get(cookie.toString());
                User loggedUser = userDAO.getUserByLogin(login);

                response = getWelcomeLayout(loggedUser);
            } else {
                response = getLoginLayout();
            }
        }
        // If the form was submitted, retrieve it's content.
        if(method.equals("POST")){

            if(isSessionValid(sessionIDs, cookie)){
                sessionIDs.remove(cookie.toString());

                response = getLoginLayout();
            } else {
                Map inputs = getFormData(httpExchange);
                User accesingUser = new User(inputs.get("login").toString(), inputs.get("password").toString());
                User userToCompare = userDAO.getUserByLogin(accesingUser.login);

                response = loginValidation(accesingUser, userToCompare, cookie);
            }
        }

        httpExchange.sendResponseHeaders(200, response.length());
        OutputStream os = httpExchange.getResponseBody();
        os.write(response.getBytes());
        os.close();
    }

    private Map<String, String> getFormData(HttpExchange httpExchange) throws IOException{
            InputStreamReader isr = new InputStreamReader(httpExchange.getRequestBody(), "utf-8");
            BufferedReader br = new BufferedReader(isr);
            String formData = br.readLine();

            System.out.println(formData);
            Map inputs = parseFormData(formData);

            return inputs;
    }

    private boolean isSessionValid(Map<String, String> sessionIDs, HttpCookie cookie) {
        if(sessionIDs.containsKey(cookie.toString())) {
            return true;
        } else {
            return false;
        }
    }
    private String getWelcomeLayout(User loggedUser) {
        String response;
        JtwigTemplate template = JtwigTemplate.classpathTemplate("templates/welcomepage.twig");
        JtwigModel model = JtwigModel.newModel();

        model.with("login", loggedUser.login);

        response = template.render(model);

        return response;
    }

    private String getLoginLayout() {
        String response;

        JtwigTemplate template = JtwigTemplate.classpathTemplate("templates/loginpage.twig");
        JtwigModel model = JtwigModel.newModel();

        response = template.render(model);

        return response;
    }

    private HttpCookie getCookie(HttpExchange httpExchange, HttpCookie cookie) {
        UUID sessionID = UUID.randomUUID();
        String cookieStr = httpExchange.getRequestHeaders().getFirst("Cookie");

        if (cookieStr != null) {  // Cookie already exists
            cookie = HttpCookie.parse(cookieStr).get(0);
            System.out.println(cookie.toString());
        } else { // Create a new cookie
            cookie = new HttpCookie("sessionId", String.valueOf(sessionID));
            httpExchange.getResponseHeaders().add("Set-Cookie", cookie.toString());
        }

        return cookie;
    }

    private String loginValidation(User accesingUser, User userToCompare, HttpCookie cookie) {
            String response = "";

            if(userToCompare == null || !userToCompare.password.equals(accesingUser.password)) {
                response = getLoginLayout();
                response += "Type correct Login & Password <br />";
            } else if (accesingUser.password.equals(userToCompare.password)) {
                sessionIDs.put(cookie.toString(), accesingUser.login);

                response = getWelcomeLayout(accesingUser);
            }
        return response;
    }

    private static Map<String, String> parseFormData(String formData) throws UnsupportedEncodingException {
        Map<String, String> map = new HashMap<>();
        String[] pairs = formData.split("&");
        for(String pair : pairs){
            String[] keyValue = pair.split("=");
            // We have to decode the value because it's urlencoded. see: https://en.wikipedia.org/wiki/POST_(HTTP)#Use_for_submitting_web_forms
            String value = new URLDecoder().decode(keyValue[1], "UTF-8");
            map.put(keyValue[0], value);
        }
        return map;
    }
}