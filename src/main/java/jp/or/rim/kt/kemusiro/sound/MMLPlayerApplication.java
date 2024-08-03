/*
 * http://asamomiji.jp/contents/mml-player
 */

package jp.or.rim.kt.kemusiro.sound;

import java.awt.Container;
import javax.swing.JDesktopPane;
import javax.swing.JFrame;


/**
 * Play MML.
 *
 * @author Kenichi Miyata (kemusiro&#x40;kt.rim.or.jp)
 * @version $Revision$
 */
public class MMLPlayerApplication extends JFrame {

    final Container container;
    final JDesktopPane desktop;

    public MMLPlayerApplication() {
        super("MML Player");

        container = this.getContentPane();
        desktop = new JDesktopPane();
        container.add(desktop);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(400, 300);
        setVisible(true);
    }

    public static void main(String[] args) {
        MMLPlayerApplication frame = new MMLPlayerApplication();
    }
}
