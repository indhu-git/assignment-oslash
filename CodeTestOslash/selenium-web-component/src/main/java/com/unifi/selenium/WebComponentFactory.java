package com.unifi.selenium;

import org.openqa.selenium.WebElement;

public interface WebComponentFactory {

    <T extends WebComponent> T create(Class<T> webComponentClass, WebElement webElement);
}