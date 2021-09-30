package com.unifi.selenium;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import java.lang.reflect.ParameterizedType;
import java.util.List;

public interface WebComponentListFactory {

    <T extends WebComponent> List<T> create(Class<T> webComponentClass, List<WebElement> webElements, WebDriver driver, ParameterizedType genericTypeArguments);
}