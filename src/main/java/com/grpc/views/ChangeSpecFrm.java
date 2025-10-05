package com.grpc.views;

import com.grpc.client.GrpcChatClient;
import net.miginfocom.swing.MigLayout;

import javax.swing.*;
import java.awt.*;

public class ChangeSpecFrm extends JFrame {
    JTextField txtUsername;
    JTextField txtColor;
    ISpecChangeHandler handler;
    public ChangeSpecFrm(ISpecChangeHandler handler) {
        super();

        this.handler = handler;
        this.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

        this.setTitle("Change client specification");
        this.setSize(new Dimension(400, 150));
        this.setLocationRelativeTo(null);
        JPanel pnlMain = new JPanel(new MigLayout("fillx"));
        txtUsername = new JTextField();
        txtUsername.setText(GrpcChatClient.getUsername());
        txtColor = new JTextField();
        txtColor.setText(GrpcChatClient.getColor());
        JButton btnConfirm = new JButton("Confirm");
        btnConfirm.addActionListener(e -> updateClientSpec());

        pnlMain.add(new JLabel("Username:"), "");
        pnlMain.add(txtUsername, "grow, pushx, wrap");
        pnlMain.add(new JLabel("Color"));
        pnlMain.add(txtColor, "grow, pushx, wrap");
        pnlMain.add(btnConfirm, "span, right");
        this.add(pnlMain);
    }

    void updateClientSpec() {
        GrpcChatClient.setUsername(txtUsername.getText());
        GrpcChatClient.setColor(txtColor.getText());
        handler.onSpecChange();

        this.dispose();
    }
}
