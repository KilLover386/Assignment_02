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
    
  public main_GLEventListener(Camera camera) {
    this.camera = camera;
    this.camera.setPosition(new Vec3(4f,6f,15f));
    this.camera.setTarget(new Vec3(0f,0,0f));
  }
  
  public void init(GLAutoDrawable drawable) {
    GL3 gl = drawable.getGL().getGL3();
    System.err.println("Chosen GLCapabilities: " + drawable.getChosenGLCapabilities());
    gl.glClearColor(0.0f, 0.0f, 0.0f, 1.0f); 
    gl.glClearDepth(1.0f);
    gl.glEnable(GL.GL_DEPTH_TEST);
    gl.glDepthFunc(GL.GL_LESS);
    gl.glFrontFace(GL.GL_CCW);    
    gl.glEnable(GL.GL_CULL_FACE); 
    gl.glCullFace(GL.GL_BACK);    
    initialise(gl);
    startTime = getSeconds();
  }
  
  public void reshape(GLAutoDrawable drawable, int x, int y, int width, int height) {
    GL3 gl = drawable.getGL().getGL3();
    gl.glViewport(x, y, width, height);
    float aspect = (float)width/(float)height;
    Mat4 p = Mat4Transform.perspective(45, aspect);
    camera.setPerspectiveMatrix(p);
  }

  public void display(GLAutoDrawable drawable) {
    GL3 gl = drawable.getGL().getGL3();
    render(gl);
  }

  public void dispose(GLAutoDrawable drawable) {
  }

  // ***************************************************
  // THE SCENE

  private Model statue1, statue2, statue3;
  // Positions for proximity check
  private Vec3 statue1Pos = new Vec3(3, 0.5f, 0);
  private Vec3 statue2Pos = new Vec3(-2f, 0.5f, 2f);
  private Vec3 statue3Pos = new Vec3(-2f, 0.5f, -2f);

  private Floor floor;
  private Walls walls;
  private Model sun;
  private Light light;
  private Bee bee;

  private TextureLibrary textures;

  // Animation Variables for Bee Path
  private Vec3[] waypoints;
  private int currentWaypointIndex = 0;
  private float travelProgress = 0.0f;
  private float travelSpeed = 0.3f; // Adjust to speed up/slow down bee movement

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

    // Initialize waypoints
    waypoints = new Vec3[] {
      new Vec3(5, 2, 5),
      new Vec3(5, 2, -5),
      new Vec3(-5, 2, -5),
      new Vec3(-5,2, 5)
    };

    light = new Light(gl, camera);
    Material material = new Material();
    material.setAmbient(0.15f, 0.15f, 0.15f);   
    material.setDiffuse(0.8f, 0.75f, 0.6f);     
    material.setSpecular(1.2f, 1.2f, 1.0f);     
    light.setMaterial(material);
    light.setPosition(0f, 25f, 0f);           

    floor = new Floor(gl, light, camera, textures.get("snow_ground"));
    walls = new Walls(gl, light, camera, textures);
    bee = new Bee(gl, light, camera, textures);

    // Statue 1
    Mat4 mSphere = Mat4Transform.translate(statue1Pos);
    mSphere = Mat4.multiply(Mat4Transform.scale(2,4,2),mSphere);
    statue1 = makeSphere(gl, mSphere, "assets/shaders/fs_standard_d.txt", textures.get("cloud"), null, null);

    // Statue 2
    Mat4 mSphere2 = Mat4Transform.translate(statue2Pos);
    mSphere2 = Mat4.multiply(Mat4Transform.scale(1.5f,3f,1.5f),mSphere2);
    statue2 = makeSphere(gl, mSphere2, "assets/shaders/fs_standard_d.txt", textures.get("matrix"), null, null);

    // Statue 3
    Mat4 mSphere3 = Mat4Transform.translate(statue3Pos);
    mSphere3 = Mat4.multiply(Mat4Transform.scale(2f,4f,2f),mSphere3);
    statue3 = makeSphere(gl, mSphere3, "assets/shaders/fs_standard_d.txt", textures.get("dog"), null, null);

    // Sun
    Mat4 mSun = Mat4Transform.translate(0f, 25f, 0f);
    mSun = Mat4.multiply(Mat4Transform.scale(1f, 1f, 1f), mSun);
    sun = makeSphere(gl, mSun, "assets/shaders/fs_standard_e.txt", textures.get("white1x1"), null, textures.get("white1x1"));
  
    // Initial bee setup
    bee.setPosition(waypoints[0].x, waypoints[0].y, waypoints[0].z);              
    bee.setRotationAngle(90f);
    bee.setRotationAngle(180+90f);                   
  }

  public void render(GL3 gl) {
    gl.glClear(GL.GL_COLOR_BUFFER_BIT | GL.GL_DEPTH_BUFFER_BIT);
    
    // Update Bee Movement
    updateBeeMovement();
    
    // Check Proximity
    checkProximity();

    light.render(gl);
    floor.render(gl);
    walls.render(gl);
    statue1.render(gl);
    statue2.render(gl);
    statue3.render(gl);
    
    bee.render(gl);
  }

  private void updateBeeMovement() {
    double elapsedTime = getSeconds() - startTime;
    double dt = 0.016; // Approx delta time for 60fps

    // Path Logic
    travelProgress += travelSpeed * dt;
    if (travelProgress >= 1.0f) {
        travelProgress = 0.0f;
        currentWaypointIndex = (currentWaypointIndex + 1) % waypoints.length;
    }

    Vec3 start = waypoints[currentWaypointIndex];
    Vec3 end = waypoints[(currentWaypointIndex + 1) % waypoints.length];

    // Linear Interpolation (Lerp)
    float x = start.x + (end.x - start.x) * travelProgress;
    float z = start.z + (end.z - start.z) * travelProgress;
    
    // Base Y + Wobble
    float baseY = start.y + (end.y - start.y) * travelProgress;
    float wobble = (float)Math.sin(elapsedTime * 2.0) * 0.5f; // Speed 2.0, Height 0.5
    float y = baseY + wobble;

    bee.setPosition(x, y, z);
  }

  private void checkProximity() {
    Vec3 beePos = bee.getPosition();
    float threshold = 2.5f; // Distance to trigger texture change

    // Helper to check dist and swap texture
    updateStatueTexture(statue1, statue1Pos, beePos, "cloud", threshold);
    updateStatueTexture(statue2, statue2Pos, beePos, "matrix", threshold);
    updateStatueTexture(statue3, statue3Pos, beePos, "dog", threshold);
  }

  private void updateStatueTexture(Model statue, Vec3 statuePos, Vec3 beePos, String originalTex, float threshold) {
    // Distance formula: sqrt((x2-x1)^2 + ...)
    float dx = statuePos.x - beePos.x;
    float dy = statuePos.y - beePos.y;
    float dz = statuePos.z - beePos.z;
    float dist = (float)Math.sqrt(dx*dx + dy*dy + dz*dz);

    if (dist < threshold) {
      statue.getMaterial().setDiffuseMap(textures.get("beeSkin"));
    } else {
      statue.getMaterial().setDiffuseMap(textures.get(originalTex));
    }
  }

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

  public void setBeePosition(float x, float y, float z) {
    bee.setPosition(x, y, z);
  }
  
  public void setBeeRotation(float angle) {
    bee.setRotationAngle(angle);
  }

  private double startTime;
  
  private double getSeconds() {
    return System.currentTimeMillis()/1000.0;
  }

  private int NUM_RANDOMS = 1000;
  private float[] randoms;
  
  private void createRandomNumbers() {
    randoms = new float[NUM_RANDOMS];
    for (int i=0; i<NUM_RANDOMS; ++i) {
      randoms[i] = (float)Math.random();
    }
  }
}