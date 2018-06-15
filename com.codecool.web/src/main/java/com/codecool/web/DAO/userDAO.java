package com.codecool.web.DAO;

import com.codecool.web.Connection.ConnectionBuilder;
import com.codecool.web.Model.User;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class userDAO {

    private static final String GET_USER_BY_LOGIN =
            "SELECT login, password\n" +
            "FROM logindata\n" +
            "WHERE login = ?;";

    public User getUserByLogin(String login) {
        try {
            Connection connection = ConnectionBuilder.getConnection();
            PreparedStatement preparedStatement = connection.prepareStatement(GET_USER_BY_LOGIN);

            preparedStatement.setString(1, login);

            ResultSet resultSet = preparedStatement.executeQuery();

            if(resultSet.next()) {
                User user = extractUserFromRow(resultSet);
                connection.close();
                preparedStatement.close();
                return user;
            } else {
                return null;
            }

        } catch(SQLException e) {
            e.printStackTrace();
        }

        return null;
    }

    private User extractUserFromRow(ResultSet resultSet) throws SQLException{
        String login = resultSet.getString("login");
        String password = resultSet.getString("password");

        User user = new User(login, password);

        return user;
    }
}
