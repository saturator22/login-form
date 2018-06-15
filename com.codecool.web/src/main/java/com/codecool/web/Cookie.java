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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class Cookie implements HttpHandler {
    int counter = 0;

    @Override
    public void handle(HttpExchange httpExchange) throws IOException {
        UUID sessionID = UUID.randomUUID();
        counter++;

        String method = httpExchange.getRequestMethod();
        String response = "";

        String cookieStr = httpExchange.getRequestHeaders().getFirst("Cookie");
        HttpCookie cookie;

        userDAO userDAO = new userDAO();

        if (cookieStr != null) {  // Cookie already exists
            cookie = HttpCookie.parse(cookieStr).get(0);
            System.out.println(cookie.toString());
        } else { // Create a new cookie
            cookie = new HttpCookie("sessionId", String.valueOf(sessionID)); // This isn't a good way to create sessionId. Find out better!
            httpExchange.getResponseHeaders().add("Set-Cookie", cookie.toString());
        }

        // Send a form if it wasn't submitted yet.
        if(method.equals("GET")){

            JtwigTemplate template = JtwigTemplate.classpathTemplate("templates/loginpage.twig");

            JtwigModel model = JtwigModel.newModel();

            response = template.render(model);
        }

        // If the form was submitted, retrieve it's content.
        if(method.equals("POST")){

            InputStreamReader isr = new InputStreamReader(httpExchange.getRequestBody(), "utf-8");
            BufferedReader br = new BufferedReader(isr);
            String formData = br.readLine();

            System.out.println(formData);
            Map inputs = parseFormData(formData);

            User accessingUser = new User(inputs.get("login").toString(), inputs.get("password").toString());

            User userToCompare = userDAO.getUserByLogin(accessingUser.login);
            System.out.println("Accessing User: " + accessingUser.login +
                               "\nPassword: " + accessingUser.password +
                               "\nUser to compare: " + userToCompare.login +
                               "\nPassword: " + userToCompare.password);

            if(userToCompare == null || !userToCompare.password.equals(accessingUser.password)) {
                JtwigTemplate template = JtwigTemplate.classpathTemplate("templates/loginpage.twig");
                JtwigModel model = JtwigModel.newModel();
                response = template.render(model);
                response += "Type correct Login & Password <br />";
            } else if (accessingUser.password.equals(userToCompare.password)) {
                JtwigTemplate template = JtwigTemplate.classpathTemplate("templates/welcomepage.twig");
                JtwigModel model = JtwigModel.newModel();

                model.with("login", accessingUser.login);

                response = template.render(model);
            }
//            Login post = new Login(inputs.get("Message").toString(), inputs.get("Name").toString(),
//                    inputs.get("Email").toString(), null);
//
//            System.out.println(post.email);
//            System.out.println(post.name);
//            System.out.println(post.message);

//            postDao.insertPost(post);

        }

        response += "Page was visited: " + counter + " times!";
        response += "<br /> session id: " + cookie.getValue();

        httpExchange.sendResponseHeaders(200, response.length());
        OutputStream os = httpExchange.getResponseBody();
        os.write(response.getBytes());
        os.close();
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