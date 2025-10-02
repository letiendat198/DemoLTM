package com.grpc.views;

import net.miginfocom.swing.MigLayout;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class ChatFrm extends JFrame {
    JPanel pnlMain;
    JPanel pnlChat;
    JTextField txtMsg;
    JLabel lblStatus;

    public ChatFrm() {
        super();
        this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        this.setTitle("LTM gRPC Demo");

        pnlMain = new JPanel(new MigLayout("fillx"));
        pnlMain.add(new JLabel("LTM gRPC Demo"), "align center, span, wrap");

        lblStatus = new JLabel("<html>Status: <font color='#B8860B'>Connecting to server...</font></html>");
        pnlMain.add(lblStatus, "wrap");

        pnlChat = new JPanel(new MigLayout("fillx"));
        pnlChat.setBackground(Color.WHITE);
        pnlChat.setBorder(BorderFactory.createLineBorder(Color.BLACK));
        pnlMain.add(pnlChat, "grow, push, span, wrap");

        txtMsg = new JTextField();
        JButton btnSend = new JButton("Send");
        btnSend.addActionListener(e -> sendOnClick(e));
        pnlMain.add(txtMsg, "grow, pushx");
        pnlMain.add(btnSend, "right, wrap");

        this.add(pnlMain);
    }

    public void addMessage(String user, String msg) {
        JLabel lblMsg = new JLabel(String.format("<html><font color='red'>%s: </font>%s</html>", user, msg));
        pnlChat.add(lblMsg, "wrap");
        pnlChat.revalidate();
    }

    public void setStatus(String status, String color) {
        lblStatus.setText(String.format("<html>Status: <font color='%s'>%s</font></html>", color, status));
    }

    private void sendOnClick(ActionEvent e) {
        addMessage("Self", txtMsg.getText());
        txtMsg.setText("");
    }
}
