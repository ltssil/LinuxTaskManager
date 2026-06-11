package cn.ltssil.linux;

import cn.ltssil.linux.ui.MainFrame;

import javax.swing.*;

public class Main {

    public static void main(String[] args) {

        SwingUtilities.invokeLater(() ->
                new MainFrame()
        );
    }
}