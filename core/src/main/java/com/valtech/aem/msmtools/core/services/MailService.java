package com.valtech.aem.msmtools.core.services;

import org.apache.commons.mail.EmailException;

public interface MailService {

  void send(String fromMail, String[] toMail, String subject, String htmlContent) throws EmailException;
}
