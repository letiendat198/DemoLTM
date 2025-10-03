package com.grpc.views;

import com.grpc.client.GrpcChatClient;
import com.grpc.client.IClientHandler;
import com.grpc.models.buffer.ChatMessage;
import com.grpc.models.buffer.ClientSpec;
import com.grpc.models.buffer.MESSAGE_TYPE;
import net.miginfocom.swing.MigLayout;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ItemEvent;
import java.net.InetAddress;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ChatFrm extends JFrame implements IClientHandler {
    JPanel pnlMain;
    JPanel pnlChat;
    JTextField txtMsg;
    JLabel lblStatus;
    JComboBox<String> cmbTarget;
    GrpcChatClient client;

    Map<String, ClientSpec> targetMap;
    ClientSpec target;

    public ChatFrm() {
        super();

        this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        this.setTitle("LTM gRPC Demo");

        pnlMain = new JPanel(new MigLayout("fillx"));
        pnlMain.add(new JLabel("LTM gRPC Demo"), "align center, span, wrap");

        lblStatus = new JLabel("<html>Status: <font color='#B8860B'>Connecting to server...</font></html>");
        pnlMain.add(lblStatus);
        JButton btnReload = new JButton("Reload");
        btnReload.addActionListener(e -> {
            setStatus("Connecting to server...", "#B8860B");
            client.register(client.thisClient.getUsername(), client.thisClient.getColor());
        });
        JButton btnChangeUsername = new JButton("Change username");
        pnlMain.add(btnReload, "right");
        pnlMain.add(btnChangeUsername, "wrap");

        cmbTarget = new JComboBox<>();
        cmbTarget.addItemListener(e -> { // ActionListener will be fired for all changes, including add or remove
            if (e.getStateChange() == ItemEvent.SELECTED) {
                String selected = (String) cmbTarget.getModel().getSelectedItem();
                String uuid = selected.substring(selected.indexOf("(") + 1, selected.indexOf(")"));
                if (!uuid.equalsIgnoreCase("Broadcast")) target = targetMap.get(uuid);
                else target = null;
            }
        });
        pnlMain.add(new JLabel("Target:"), "split 2");
        pnlMain.add(cmbTarget, "grow, pushx, span 2, wrap");


        pnlChat = new JPanel(new MigLayout("fillx"));
        pnlChat.setBackground(Color.WHITE);
        pnlChat.setBorder(BorderFactory.createLineBorder(Color.BLACK));
        JScrollPane scrollPane = new JScrollPane(pnlChat);
        pnlMain.add(scrollPane, "grow, push, span, wrap");

        txtMsg = new JTextField();
        JButton btnSend = new JButton("Send");
        btnSend.addActionListener(e -> sendOnClick(e));
        JPanel pnlRow = new JPanel(new MigLayout("fillx"));
        pnlRow.add(txtMsg, "grow, pushx");
        pnlRow.add(btnSend, "right, wrap");
        pnlMain.add(pnlRow, "grow, span");

        this.add(pnlMain);

        initClient();
    }

    void initClient() {
        try {
            client = new GrpcChatClient();
            client.setHandler(this);
            // Default register value: PC name + blue
            client.register(InetAddress.getLocalHost().getHostName(), "blue");
        }
        catch (Exception e) {
            System.out.println(e);
        }
    }

    public void addMessage(String user, String msg, String color) {
        JLabel lblMsg = new JLabel(String.format("<html><font color='%s'>%s: </font>%s</html>", color, user, msg));
        pnlChat.add(lblMsg, "wrap");
        pnlChat.revalidate();
    }

    public void setStatus(String status, String color) {
        lblStatus.setText(String.format("<html>Status: <font color='%s'>%s</font></html>", color, status));
    }

    public void updateTargetComboBox() {
        cmbTarget.removeAllItems();
        cmbTarget.addItem("All targets (Broadcast)");
        for (String key: targetMap.keySet()) {
            String username = targetMap.get(key).getUsername();

            cmbTarget.addItem(username + " (" + key + ")");
        }
    }

    private void sendOnClick(ActionEvent e) {
        addMessage(client.thisClient.getUsername(), txtMsg.getText(), client.thisClient.getColor());

        ChatMessage msg = ChatMessage.newBuilder()
                .setSourceUUID(client.uuid)
                .setTargetUUID(target == null ? "" : target.getUuid())
                .setMessage(txtMsg.getText())
                .setType(target == null ? MESSAGE_TYPE.MESSAGE_TYPE_BROADCAST : MESSAGE_TYPE.MESSAGE_TYPE_UNICAST)
                .setTimestamp(Instant.now().getEpochSecond())
                .build();
        client.sendMessage(msg);

        txtMsg.setText("");
    }

    @Override
    public void onMessage(ChatMessage msg) {
        if (msg.getSourceUUID().equals(client.thisClient.getUuid())) {
            System.out.println("Loopback message! Ignore");
            return;
        }

        if (targetMap.containsKey(msg.getSourceUUID())) {
            ClientSpec src = targetMap.get(msg.getSourceUUID());
            addMessage(src.getUsername(), msg.getMessage(), src.getColor());
        }
        else addMessage("Unknown: " + msg.getSourceUUID(), msg.getMessage(), "red");
    }

    @Override
    public void onRegisterComplete(boolean success, Map<String, ClientSpec> targetMap) {
        if (success) {
            setStatus("Connected", "green");
            this.targetMap = targetMap;
            updateTargetComboBox();
        }
        else {
            setStatus("Client initialization failed or server not found!", "red");
        }
    }
}
