package com.grpc;

import com.grpc.views.ChatFrm;

import javax.swing.*;
import java.awt.*;
import java.net.InetAddress;
import java.net.UnknownHostException;

public class Main {
    public static void main(String[] args) {
        JFrame frmMain = new ChatFrm();
        frmMain.setSize(new Dimension(800, 600));
        frmMain.setLocationRelativeTo(null);
        frmMain.setVisible(true);
    }
}
