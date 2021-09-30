package com.unifi.selenium;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import java.lang.reflect.InvocationTargetException;

public class DefaultWebComponentFactory implements WebComponentFactory {

    private WebDriver driver;
    public DefaultWebComponentFactory(WebDriver driver) {
        this.driver = driver;
    }

    @Override
    public <T extends WebComponent> T create(Class<T> webComponentClass, WebElement webElement) {
        return createInstanceOf(webComponentClass, webElement);
    }

    private <T extends WebComponent> T createInstanceOf(final Class<T> webComponentClass, final WebElement webElement) {
        try {
            T webComponent;
            try {
                webComponent = (T) webComponentClass.getConstructor(WebDriver.class).newInstance(driver);
            } catch (NoSuchMethodException|InvocationTargetException e1) {
                webComponent = (T) webComponentClass.newInstance();
            }
            webComponent.init(webElement);
            return webComponent;
        } catch (IllegalArgumentException|SecurityException|InstantiationException|IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }
}