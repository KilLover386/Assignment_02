import java.awt.*;
import java.awt.event.*;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JLabel;

import com.jogamp.opengl.*;
import com.jogamp.opengl.awt.GLCanvas;
import com.jogamp.opengl.util.FPSAnimator;
import gmaths.*;

public class Buzz extends JFrame implements ActionListener {
  
  private static final int WIDTH = 1024;
  private static final int HEIGHT = 768;
  private static final Dimension dimension = new Dimension(WIDTH, HEIGHT);
  private GLCanvas canvas;
  private main_GLEventListener glEventListener;
  private final FPSAnimator animator; 

  public static void main(String[] args) {
    Buzz b1 = new Buzz("Assignment 1 - Buzz");
    b1.getContentPane().setPreferredSize(dimension);
    b1.pack();
    b1.setVisible(true);
    b1.canvas.requestFocusInWindow();
  }

  public Buzz(String textForTitleBar) {
    super(textForTitleBar);
    GLCapabilities glcapabilities = new GLCapabilities(GLProfile.get(GLProfile.GL3));
    canvas = new GLCanvas(glcapabilities);
    Camera camera = new Camera(Camera.DEFAULT_POSITION, Camera.DEFAULT_TARGET, Camera.DEFAULT_UP);
    glEventListener = new main_GLEventListener(camera);
    canvas.addGLEventListener(glEventListener);
    canvas.addMouseMotionListener(new MyMouseInput(camera));
    canvas.addKeyListener(new MyKeyboardInput(camera));
    getContentPane().add(canvas, BorderLayout.CENTER);

    JPanel p = new JPanel();
      JButton b = new JButton("Light On/Off");
      b.addActionListener(this);
      p.add(b);
      b = new JButton("Spotlight Bulb");
      b.setActionCommand("SpotLight");
      b.addActionListener(this);
      p.add(b);
      b = new JButton("Spotlight Motor");
      b.setActionCommand("SpotMotor");
      b.addActionListener(this);
      p.add(b);
      b = new JButton("Toggle Animation/Pose");
      b.addActionListener(this);
      p.add(b);
      
      p.add(new JLabel(" | Poses: "));
      
      b = new JButton("Statue 1");
      b.setActionCommand("Pose1");
      b.addActionListener(this);
      p.add(b);
      
      b = new JButton("Statue 2");
      b.setActionCommand("Pose2");
      b.addActionListener(this);
      p.add(b);
      
      b = new JButton("Statue 3");
      b.setActionCommand("Pose3");
      b.addActionListener(this);
      p.add(b);

    this.add(p, BorderLayout.SOUTH);

    addWindowListener(new WindowAdapter() {
      public void windowClosing(WindowEvent e) {
        animator.stop();
        remove(canvas);
        dispose();
        System.exit(0);
      }
    });
    animator = new FPSAnimator(canvas, 60);
    animator.start();
  }

  @Override
  public void actionPerformed(ActionEvent e) {
    String cmd = e.getActionCommand();
    
    if (cmd.equalsIgnoreCase("Light On/Off")) {
       glEventListener.toggleGlobalLight();
    }
    else if (cmd.equalsIgnoreCase("SpotLight")) { // Changed command
       glEventListener.toggleSpotLight();
    }
    else if (cmd.equalsIgnoreCase("SpotMotor")) { // New command
       glEventListener.toggleSpotAnim();
    }
    else if (cmd.equalsIgnoreCase("Toggle Animation/Pose")) {
       glEventListener.toggleAnimationMode();
    }
    else if (cmd.equalsIgnoreCase("Pose1")) {
       glEventListener.jumpToPose(0); // Req 3.8: Jump to statue 1
    }
    else if (cmd.equalsIgnoreCase("Pose2")) {
       glEventListener.jumpToPose(1); // Jump to statue 2
    }
    else if (cmd.equalsIgnoreCase("Pose3")) {
       glEventListener.jumpToPose(2); // Jump to statue 3
    }
  }
}

class MyKeyboardInput extends KeyAdapter  {
  private Camera camera;
  public MyKeyboardInput(Camera camera) { this.camera = camera; }
  public void keyPressed(KeyEvent e) {
    Camera.Movement m = Camera.Movement.NO_MOVEMENT;
    if (e.getModifiersEx() == java.awt.event.InputEvent.SHIFT_DOWN_MASK)
      switch (e.getKeyCode()) {
        case KeyEvent.VK_W: m = Camera.Movement.FAST_FORWARD;  break;
        case KeyEvent.VK_S: m = Camera.Movement.FAST_BACK;  break;
        case KeyEvent.VK_A: m = Camera.Movement.FAST_LEFT;  break;
        case KeyEvent.VK_D: m = Camera.Movement.FAST_RIGHT; break;
        case KeyEvent.VK_Q: m = Camera.Movement.FAST_UP;  break;
        case KeyEvent.VK_E: m = Camera.Movement.FAST_DOWN;  break;
      }
    else
      switch (e.getKeyCode()) {
        case KeyEvent.VK_W: m = Camera.Movement.FORWARD;  break;
        case KeyEvent.VK_S: m = Camera.Movement.BACK;  break;
        case KeyEvent.VK_A: m = Camera.Movement.LEFT;  break;
        case KeyEvent.VK_D: m = Camera.Movement.RIGHT; break;
        case KeyEvent.VK_Q: m = Camera.Movement.UP;  break;
        case KeyEvent.VK_E: m = Camera.Movement.DOWN;  break;
      }
    camera.keyboardInput(m);
  }
}

class MyMouseInput extends MouseMotionAdapter {
  private Point lastpoint;
  private Camera camera;
  public MyMouseInput(Camera camera) { this.camera = camera; }
  public void mouseDragged(MouseEvent e) {
    Point ms = e.getPoint();
    float sensitivity = 0.001f;
    float dx=(float) (ms.x-lastpoint.x)*sensitivity;
    float dy=(float) (ms.y-lastpoint.y)*sensitivity;
    int mask = MouseEvent.BUTTON1_DOWN_MASK & MouseEvent.SHIFT_DOWN_MASK;
    if (e.getModifiersEx()==MouseEvent.BUTTON1_DOWN_MASK
        || (e.getModifiersEx() & mask) == mask) {
      camera.updateYawPitch(dx, -dy);
    }
    lastpoint = ms;
  }
  public void mouseMoved(MouseEvent e) { lastpoint = e.getPoint(); }
}