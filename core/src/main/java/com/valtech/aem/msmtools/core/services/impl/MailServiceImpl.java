package com.valtech.aem.msmtools.core.services.impl;

import com.day.cq.mailer.MessageGateway;
import com.day.cq.mailer.MessageGatewayService;
import com.valtech.aem.msmtools.core.services.MailService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.mail.EmailException;
import org.apache.commons.mail.HtmlEmail;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.propertytypes.ServiceDescription;

@Component(immediate = true, service = MailService.class)
@ServiceDescription("Valtech - MSM Tools - Mail Service")
@Slf4j
public class MailServiceImpl implements MailService {

  @Reference
  private MessageGatewayService messageGatewayService;

  @Override
  public void send(String fromMail, String[] toMail, String subject, String htmlContent)
      throws EmailException {
    HtmlEmail htmlEmail = new HtmlEmail();
    htmlEmail.addTo(toMail);
    htmlEmail.setSubject(subject);
    htmlEmail.setFrom(fromMail);
    htmlEmail.setMsg(htmlContent);
    MessageGateway<HtmlEmail> messageGateway = messageGatewayService.getGateway(HtmlEmail.class);
    messageGateway.send(htmlEmail);
  }
}
