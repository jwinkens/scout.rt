/*******************************************************************************
 * Copyright (c) 2010-2017 BSI Business Systems Integration AG.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     BSI Business Systems Integration AG - initial API and implementation
 ******************************************************************************/
package org.eclipse.scout.rt.server.services.common.imap;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import javax.mail.Flags.Flag;
import javax.mail.Folder;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.Store;

import org.eclipse.scout.rt.platform.Order;
import org.eclipse.scout.rt.platform.annotations.ConfigProperty;
import org.eclipse.scout.rt.platform.config.CONFIG;
import org.eclipse.scout.rt.platform.config.IConfigProperty;
import org.eclipse.scout.rt.platform.exception.ProcessingException;
import org.eclipse.scout.rt.platform.util.StringUtility;
import org.eclipse.scout.rt.server.ServerConfigProperties.ImapHostProperty;
import org.eclipse.scout.rt.server.ServerConfigProperties.ImapMailboxProperty;
import org.eclipse.scout.rt.server.ServerConfigProperties.ImapPasswordProperty;
import org.eclipse.scout.rt.server.ServerConfigProperties.ImapPortProperty;
import org.eclipse.scout.rt.server.ServerConfigProperties.ImapSslProtocolsProperty;
import org.eclipse.scout.rt.server.ServerConfigProperties.ImapUsernameProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @deprecated See {@link IIMAPService}.
 */
@SuppressWarnings("deprecation")
@Deprecated
public abstract class AbstractIMAPService implements IIMAPService {

  private static final Logger LOG = LoggerFactory.getLogger(AbstractIMAPService.class);

  private final String m_host;
  private final int m_port;
  private final String m_mailbox;
  private final String m_username;
  private final String m_password;
  private final String m_sslProtocols;

  private boolean m_opened;
  private Folder m_folder;
  private Store m_store;

  public AbstractIMAPService() {
    m_host = getPropertyValue(ImapHostProperty.class, getConfiguredHost());
    m_port = getPropertyValue(ImapPortProperty.class, getConfiguredPort());
    m_mailbox = getPropertyValue(ImapMailboxProperty.class, getConfiguredMailbox());
    m_username = getPropertyValue(ImapUsernameProperty.class, getConfiguredUserName());
    m_password = getPropertyValue(ImapPasswordProperty.class, getConfiguredPassword());
    m_sslProtocols = getPropertyValue(ImapSslProtocolsProperty.class, getConfiguredSslProtocols());
    m_opened = false;
  }

  protected <DATA_TYPE> DATA_TYPE getPropertyValue(Class<? extends IConfigProperty<DATA_TYPE>> clazz, DATA_TYPE defaultValue) {
    DATA_TYPE value = CONFIG.getPropertyValue(clazz);
    if (value == null) {
      return defaultValue;
    }
    return value;
  }

  @ConfigProperty(ConfigProperty.STRING)
  @Order(10)
  protected String getConfiguredHost() {
    return null;
  }

  @ConfigProperty(ConfigProperty.STRING)
  @Order(20)
  protected int getConfiguredPort() {
    return -1;
  }

  @ConfigProperty(ConfigProperty.STRING)
  @Order(25)
  protected String getConfiguredSslProtocols() {
    return null;
  }

  @ConfigProperty(ConfigProperty.STRING)
  @Order(30)
  protected String getConfiguredMailbox() {
    return null;
  }

  @ConfigProperty(ConfigProperty.STRING)
  @Order(40)
  protected String getConfiguredUserName() {
    return null;
  }

  @ConfigProperty(ConfigProperty.STRING)
  @Order(50)
  protected String getConfiguredPassword() {
    return null;
  }

  public void openConnection() {
    openConnection(false);
  }

  public void openConnection(boolean useSSL) {
    try {
      Properties props = new Properties();
      props.put("mail.transport.protocol", "imap");
      if (m_host != null) {
        props.put("mail.imap.host", m_host);
      }
      if (m_port > 0) {
        props.put("mail.imap.port", "" + m_port);
      }
      if (StringUtility.hasText(m_sslProtocols)) {
        props.put("mail.imap.ssl.protocols", m_sslProtocols);
      }
      if (m_username != null) {
        props.put("mail.imap.user", m_username);
      }
      if (useSSL) {
        props.put("mail.imap.socketFactory.class", "javax.net.ssl.SSLSocketFactory");
        props.put("mail.imap.socketFactory.fallback", "false");
        if (m_port > 0) {
          props.put("mail.imap.socketFactory.port", "" + m_port);
        }
      }
      Session session = Session.getInstance(props, null);
      m_store = session.getStore("imap");
      if (m_username != null && m_host != null) {
        m_store.connect(m_host, m_username, m_password);
      }
      else {
        m_store.connect();
      }
      if (m_mailbox != null) {
        m_folder = m_store.getFolder(m_mailbox);
      }
      else {
        m_folder = m_store.getDefaultFolder();
      }
      m_folder.open(Folder.READ_WRITE);
      m_opened = true;
    }
    catch (Exception e) {
      throw new ProcessingException("opening", e);
    }
  }

  public void closeConnection() {
    try {
      m_folder.close(true);
      m_folder = null;
      m_store.close();
      m_store = null;
      m_opened = false;
    }
    catch (MessagingException e) {
      throw new ProcessingException(e.getMessage(), e);
    }
    finally {
      if (m_folder != null) {
        try {
          m_folder.close(false);
        }
        catch (MessagingException e) {
          LOG.warn("Could not close folder", e);
        }
      }
      if (m_store != null) {
        try {
          m_store.close();
        }
        catch (MessagingException e) {
          LOG.warn("Could not close store", e);
        }
      }
    }
  }

  @Override
  public Message[] getUnreadMessages() {
    ReadMailTask task = new ReadMailTask();
    doTask(task);
    return task.getUnreadMessages();
  }

  @Override
  public void deleteAllMessages() {
    DeleteMailTask task = new DeleteMailTask(true);
    doTask(task);
  }

  @Override
  public void deleteMessages(Message... toDelete) {
    DeleteMailTask task = new DeleteMailTask(false);
    task.setMessagesToDelete(toDelete);
    doTask(task);
  }

  private void doTask(AbstractMailTask task) {
    if (!m_opened) {
      throw new ProcessingException("No connection opened");
    }
    task.doTask(m_folder);
  }

  private abstract class AbstractMailTask {
    public void doTask(Folder folder) {
    }
  }

  private class ReadMailTask extends AbstractMailTask {

    private final List<Message> m_messages = new ArrayList<>();

    @Override
    public void doTask(Folder folder) {
      try {
        Message item;
        Message[] m = folder.getMessages();
        for (int i = 0; i < Array.getLength(m); i++) {
          item = m[i];
          if (!item.isSet(Flag.SEEN)) {
            m_messages.add(item);
          }
        }
      }
      catch (MessagingException e) {
        throw new ProcessingException(e.getMessage(), e);
      }
    }

    public Message[] getUnreadMessages() {
      Message[] messageArray = new Message[m_messages.size()];
      m_messages.toArray(messageArray);
      return messageArray;
    }
  }

  private class DeleteMailTask extends AbstractMailTask {

    private Message[] m_toDelete;
    private final boolean m_deleteAll;

    DeleteMailTask(boolean all) {
      m_deleteAll = all;
    }

    @Override
    public void doTask(Folder folder) {
      try {
        Message[] m = folder.getMessages();
        if (m_deleteAll) {
          m_toDelete = folder.getMessages();
        }
        for (int i = 0; i < Array.getLength(m_toDelete); i++) {
          Message item = m_toDelete[i];
          for (int j = 0; j < Array.getLength(m); j++) {
            Message msg = m[j];
            if (item.equals(msg)) {
              msg.setFlag(Flag.DELETED, true);
            }
          }
        }
      }
      catch (MessagingException e) {
        throw new ProcessingException(e.getMessage(), e);
      }
    }

    public void setMessagesToDelete(Message[] msgs) {
      m_toDelete = msgs;
    }
  }
}