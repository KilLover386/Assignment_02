import gmaths.*;

import java.nio.*;
import com.jogamp.common.nio.*;
import com.jogamp.opengl.*;
import com.jogamp.opengl.util.*;
import com.jogamp.opengl.util.awt.*;
import com.jogamp.opengl.util.glsl.*;

import com.jogamp.opengl.util.texture.*;
  
public class main_GLEventListener implements GLEventListener {
  
  private static final boolean DISPLAY_SHADERS = false;
  private Shader shaderCube, shaderLight;
  private Camera camera;
    
  /* The constructor is not used to initialise anything */
  public main_GLEventListener(Camera camera) {
    this.camera = camera;
    this.camera.setPosition(new Vec3(4f,6f,15f));
    this.camera.setTarget(new Vec3(0f,0,0f));
  }
  
  // ***************************************************
  /*
   * METHODS DEFINED BY GLEventListener
   */

  /* Initialisation */
  public void init(GLAutoDrawable drawable) {
    GL3 gl = drawable.getGL().getGL3();
    System.err.println("Chosen GLCapabilities: " + drawable.getChosenGLCapabilities());
    gl.glClearColor(0.0f, 0.0f, 0.0f, 1.0f); 
    gl.glClearDepth(1.0f);
    gl.glEnable(GL.GL_DEPTH_TEST);
    gl.glDepthFunc(GL.GL_LESS);
    gl.glFrontFace(GL.GL_CCW);    // default is 'CCW'
    gl.glEnable(GL.GL_CULL_FACE); // default is 'not enabled'
    gl.glCullFace(GL.GL_BACK);    // default is 'back', assuming CCW
    initialise(gl);
    startTime = getSeconds();
  }
  
  /* Called to indicate the drawing surface has been moved and/or resized  */
  public void reshape(GLAutoDrawable drawable, int x, int y, int width, int height) {
    GL3 gl = drawable.getGL().getGL3();
    gl.glViewport(x, y, width, height);
    float aspect = (float)width/(float)height;
    Mat4 p = Mat4Transform.perspective(45, aspect);
    camera.setPerspectiveMatrix(p);
  }

  /* Draw */
  public void display(GLAutoDrawable drawable) {
    GL3 gl = drawable.getGL().getGL3();
    render(gl);
  }

  /* Clean up memory, if necessary */
  public void dispose(GLAutoDrawable drawable) {
    // GL3 gl = drawable.getGL().getGL3();
    // gl.glDeleteBuffers(1, vertexBufferId, 0);
    // gl.glDeleteVertexArrays(1, vertexArrayId, 0);
    // gl.glDeleteBuffers(1, elementBufferId, 0);
  }

  // ***************************************************
  /* THE SCENE
   * Now define all the methods to handle the scene.
   * This will be added to in later examples.
   */

  private Model statue1, statue2, statue3;
  private Floor floor;
  //private FObject fObj;
  //private Cuboids cuboids;
  private Walls walls;
  private Model sun;
  private Light light;
  private Bee bee;

  // Textures
  private TextureLibrary textures;

  public void initialise(GL3 gl)
  {
    createRandomNumbers();

    textures = new TextureLibrary();
    textures.add(gl, "diffuse_container", "assets/textures/container2.jpg");
    textures.add(gl, "specular_container", "assets/textures/container2_specular.jpg");
    textures.add(gl, "chequerboard", "assets/textures/chequerboard.jpg");
    textures.add(gl, "cloud", "assets/textures/cloud.jpg");
    textures.add(gl, "matrix", "assets/textures/matrix.jpg");
    textures.add(gl, "black1x1", "assets/textures/black1x1.jpg");
    textures.add(gl, "white1x1", "assets/textures/white1x1.jpg");
    textures.add(gl, "dog", "assets/textures/dog.jpg");
    textures.add(gl, "snow", "assets/textures/snow.jpg");
    textures.add(gl, "snow_ground", "assets/textures/snow_ground.jpg");
    textures.add(gl, "sky", "assets/textures/sky.jpg");
    textures.add(gl, "beeSkin", "assets/textures/beeSkin2.jpg");


    light = new Light(gl, camera);
    Material material = new Material();
    material.setAmbient(0.15f, 0.15f, 0.15f);   // much lower ambient
    material.setDiffuse(0.8f, 0.75f, 0.6f);     // reduced diffuse intensity + warm tint
    material.setSpecular(1.2f, 1.2f, 1.0f);     // smaller specular highlights
    light.setMaterial(material);
    light.setPosition(0f, 25f, 0f);           // position it higher and to the side

    floor = new Floor(gl, light, camera, textures.get("snow_ground"));
    //fObj = new FObject(gl, light, camera);
    //cuboids = new Cuboids(gl, light, camera);
    walls = new Walls(gl, light, camera, textures);
    bee = new Bee(gl, light, camera, textures);

    Mat4 mSphere = Mat4Transform.translate(3,0.5f,0);
    mSphere = Mat4.multiply(Mat4Transform.scale(2,4,2),mSphere);
    statue1 = makeSphere(gl, mSphere,
                       "assets/shaders/fs_standard_d.txt", 
                       textures.get("cloud"), null, null);

    Mat4 mSphere2 = Mat4Transform.translate(-2f,0.5f,2f);
    mSphere2 = Mat4.multiply(Mat4Transform.scale(1.5f,3f,1.5f),mSphere2);
    statue2 = makeSphere(gl, mSphere2,
                       "assets/shaders/fs_standard_d.txt", 
                       textures.get("matrix"), null, null);

    Mat4 mSphere3 = Mat4Transform.translate(-2f,0.5f,-2f);
    mSphere3 = Mat4.multiply(Mat4Transform.scale(2f,4f,2f),mSphere3);
    statue3 = makeSphere(gl, mSphere3,
                       "assets/shaders/fs_standard_d.txt", 
                       textures.get("dog"), null, null);

    // Create glowing sun sphere at light position
    Mat4 mSun = Mat4Transform.translate(0f, 25f, 0f);
    mSun = Mat4.multiply(Mat4Transform.scale(1f, 1f, 1f), mSun);
    sun = makeSphere(gl, mSun,
                     "assets/shaders/fs_standard_e.txt",  // emission shader
                     textures.get("white1x1"), null, textures.get("white1x1"));
  
    bee.setPosition(0f, 15f, 10f);              // move whole bee
    bee.setRotationAngle(90f);                   // rotate whole bee
    bee.setRotationAngle(360-90);                   // rotate whole bee
  }

  public void render(GL3 gl) {
    gl.glClear(GL.GL_COLOR_BUFFER_BIT | GL.GL_DEPTH_BUFFER_BIT);
    //light.setPosition(getLightPosition()); // changing light position each frame
    light.render(gl);
    floor.render(gl);
    walls.render(gl);
    statue1.render(gl);
    statue2.render(gl);
    statue3.render(gl);
    //cuboids.render(gl);
    //fObj.render(gl);
    //sun.render(gl);

    // Move the bee along the same path used by the (previously-moving) light.
    Vec3 movingPos = getLightPosition();
    bee.setPosition(movingPos.x, movingPos.y, movingPos.z);

    bee.render(gl);
  }
  

  // **********************************
  // interaction menu

  // public void setFAngle(float angle) {
  //   fObj.setAngle(angle);
  // }

  // public void setCAngle(float angle) {
  //   cuboids.setAngle(angle);
  // }

  // **********************************
  // Sphere

  private Model makeSphere(GL3 gl, Mat4 m, String fragmentPath, Texture diffuse, Texture specular, Texture emission) {
    String name = "sphere";
    Mesh mesh = new Mesh(gl, Sphere.vertices.clone(), Sphere.indices.clone());
    Mat4 modelMatrix = m;
    Shader shader = new Shader(gl, "assets/shaders/vs_standard.txt", fragmentPath);
    Material material = new Material(new Vec3(1.0f, 0.5f, 0.31f), new Vec3(1.0f, 0.5f, 0.31f), new Vec3(0.5f, 0.5f, 0.5f), 32.0f);
    material.setDiffuseMap(diffuse);
    material.setSpecularMap(specular);
    material.setEmissionMap(emission);
    Renderer renderer = new Renderer();
    return new Model(name, mesh, modelMatrix, shader, material, renderer, light, camera);
  }

  // **********************************
  // Light

    // The light's position is continually being changed, so needs to be calculated for each frame.
  private Vec3 getLightPosition() {
    double elapsedTime = getSeconds()-startTime;
    float x = 7.0f*(float)(Math.sin(Math.toRadians(elapsedTime*50)));
    float y = 5.4f;
    float z = -7.0f*(float)(Math.cos(Math.toRadians(elapsedTime*50)));
    return new Vec3(x,y,z);
  }
  // ***************************************************
  // Bee

  public void setBeePosition(float x, float y, float z) {
    bee.setPosition(x, y, z);
  }
  
  public void setBeeRotation(float angle) {
    bee.setRotationAngle(angle);
  }

  // ***************************************************
  // TIME
  
  private double startTime;
  
  private double getSeconds() {
    return System.currentTimeMillis()/1000.0;
  }
  
  // ***************************************************
  // An array of random numbers

  private int NUM_RANDOMS = 1000;
  private float[] randoms;
  
  private void createRandomNumbers() {
    randoms = new float[NUM_RANDOMS];
    for (int i=0; i<NUM_RANDOMS; ++i) {
      randoms[i] = (float)Math.random();
    }
  }

}
