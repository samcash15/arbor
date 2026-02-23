module com.arbor {
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.graphics;
    requires javafx.web;
    requires org.fxmisc.richtext;
    requires org.fxmisc.flowless;
    requires reactfx;
    requires com.google.gson;
    requires org.slf4j;
    requires org.commonmark;

    opens com.arbor to javafx.fxml;
    opens com.arbor.model to com.google.gson;

    exports com.arbor;
    exports com.arbor.model;
    exports com.arbor.service;
    exports com.arbor.view;
    exports com.arbor.controller;
    exports com.arbor.util;
}
