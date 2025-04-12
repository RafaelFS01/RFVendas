module BackEnd {
    requires transitive javafx.controls;
    requires transitive javafx.fxml;
    requires transitive javafx.graphics;
    requires transitive javafx.base;
    requires transitive java.sql;
    requires mysql.connector.j;

    opens BackEnd to javafx.fxml;
    opens BackEnd.controller to javafx.fxml;
    opens BackEnd.model to javafx.fxml;
    opens BackEnd.model.entity to javafx.fxml;
    opens BackEnd.model.dao.interfaces to javafx.fxml;
    opens BackEnd.model.dao.impl to javafx.fxml;
    opens BackEnd.util to javafx.fxml;

    exports BackEnd;
    exports BackEnd.controller;
    exports BackEnd.model;
    exports BackEnd.model.entity;
    exports BackEnd.model.dao.interfaces;
    exports BackEnd.model.dao.impl;
    exports BackEnd.model.service;
    exports BackEnd.util;
}