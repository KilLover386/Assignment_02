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
  
  // Toggles
  private boolean lightOn = true;
  private boolean spotOn = true;
  private boolean animationMode = true; // true = continuous, false = pose
    
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
  // Positions
  private Vec3 statue1Pos = new Vec3(3, 0.5f, 0);
  private Vec3 statue2Pos = new Vec3(-2f, 0.5f, 2f);
  private Vec3 statue3Pos = new Vec3(-2f, 0.5f, -2f);

  private Floor floor;
  private Walls walls;
  private Model sun;
  private Light light;
  private Bee bee;
  
  private Light spotlightLight;
  private Spotlight spotlightObject;

  private TextureLibrary textures;

  private Vec3[] waypoints;
  private int currentWaypointIndex = 0;
  private float travelProgress = 0.0f;
  private float travelSpeed = 0.3f;

  // Materials
  private Material mStone, mMetal, mWood;

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

    // Define Materials
    // Stone: Low specular, matte
    mStone = new Material(new Vec3(0.1f, 0.1f, 0.1f), new Vec3(0.6f, 0.6f, 0.6f), new Vec3(0.1f, 0.1f, 0.1f), 4.0f);
    // Metal: High specular, shiny
    mMetal = new Material(new Vec3(0.2f, 0.2f, 0.2f), new Vec3(0.7f, 0.7f, 0.7f), new Vec3(1.0f, 1.0f, 1.0f), 64.0f);
    // Wood: Medium specular
    mWood = new Material(new Vec3(0.1f, 0.1f, 0.1f), new Vec3(0.6f, 0.4f, 0.2f), new Vec3(0.2f, 0.2f, 0.2f), 8.0f);

    // 1. Main Light
    light = new Light(gl, camera);
    Material lightMat = new Material();
    lightMat.setAmbient(0.1f, 0.1f, 0.1f); // Low ambient so spotlight pops
    lightMat.setDiffuse(0.6f, 0.6f, 0.6f);
    lightMat.setSpecular(0.6f, 0.6f, 0.6f);
    light.setMaterial(lightMat);
    light.setPosition(0f, 25f, 0f);           

    // 2. Spotlight
    spotlightLight = new Light(gl, camera);
    Material spotMat = new Material();
    spotMat.setAmbient(0.0f, 0.0f, 0.0f); 
    spotMat.setDiffuse(1.0f, 1.0f, 1.0f); 
    spotMat.setSpecular(1.0f, 1.0f, 1.0f);
    spotlightLight.setMaterial(spotMat);
    spotlightLight.setCutoff((float)Math.cos(Math.toRadians(15.0))); 
    Renderer.spotlight = spotlightLight; // Register

    // 3. Objects
    floor = new Floor(gl, light, camera, textures.get("snow_ground"));
    walls = new Walls(gl, light, camera, textures);
    bee = new Bee(gl, light, camera, textures);
    spotlightObject = new Spotlight(gl, spotlightLight, camera, textures);

    // Statue 1 (Stone)
    Mat4 mSphere = Mat4Transform.translate(statue1Pos);
    mSphere = Mat4.multiply(Mat4Transform.scale(2,4,2),mSphere);
    statue1 = makeSphere(gl, mSphere, "assets/shaders/fs_standard_d.txt", textures.get("cloud"), null, null, mStone);

    // Statue 2 (Metal)
    Mat4 mSphere2 = Mat4Transform.translate(statue2Pos);
    mSphere2 = Mat4.multiply(Mat4Transform.scale(1.5f,3f,1.5f),mSphere2);
    statue2 = makeSphere(gl, mSphere2, "assets/shaders/fs_standard_d.txt", textures.get("matrix"), null, null, mMetal);

    // Statue 3 (Wood)
    Mat4 mSphere3 = Mat4Transform.translate(statue3Pos);
    mSphere3 = Mat4.multiply(Mat4Transform.scale(2f,4f,2f),mSphere3);
    statue3 = makeSphere(gl, mSphere3, "assets/shaders/fs_standard_d.txt", textures.get("dog"), null, null, mWood);

    // Sun
    Mat4 mSun = Mat4Transform.translate(0f, 25f, 0f);
    mSun = Mat4.multiply(Mat4Transform.scale(1f, 1f, 1f), mSun);
    sun = makeSphere(gl, mSun, "assets/shaders/fs_standard_e.txt", textures.get("white1x1"), null, textures.get("white1x1"), new Material());
  
    bee.setPosition(waypoints[0].x, waypoints[0].y, waypoints[0].z);              
    bee.setRotationAngle(90f);
    bee.setRotationAngle(180+90f);                   
  }

  public void render(GL3 gl) {
    gl.glClear(GL.GL_COLOR_BUFFER_BIT | GL.GL_DEPTH_BUFFER_BIT);
    
    // Updates
    if (spotOn) spotlightObject.update(); 
    if (animationMode) updateBeeMovement();
    
    checkProximity();

    // Render Logic based on Toggles
    if (lightOn) light.render(gl);
    
    // Pass 'null' to renderer if light is off (Renderer handles this?) 
    // Actually simpler to just set the light material properties to black temporarily
    // But for now, we rely on the Renderer using the light object we pass.
    // If we want to turn it off, we can just not render the visual sphere, but the light effect persists.
    // To truly turn off light effect, we'd modify the Light object properties.
    // A quick hack for the "visual" switch is just skipping light.render().
    // For the "lighting effect", we'd need to zero out its intensity.
    // Let's assume the button toggles the *effect*.
    
    if (!lightOn) {
        // Dim the light for this frame
        light.getMaterial().setDiffuse(0,0,0);
        light.getMaterial().setSpecular(0,0,0);
        light.getMaterial().setAmbient(0,0,0);
    } else {
        // Restore
        light.getMaterial().setAmbient(0.1f, 0.1f, 0.1f);
        light.getMaterial().setDiffuse(0.6f, 0.6f, 0.6f);
        light.getMaterial().setSpecular(0.6f, 0.6f, 0.6f);
    }
    
    spotlightLight.setOn(spotOn);

    floor.render(gl);
    walls.render(gl);
    statue1.render(gl);
    statue2.render(gl);
    statue3.render(gl);
    
    bee.render(gl);
    spotlightObject.render(gl);
  }

  private void updateBeeMovement() {
    double elapsedTime = getSeconds() - startTime;
    double dt = 0.016; 

    travelProgress += travelSpeed * dt;
    if (travelProgress >= 1.0f) {
        travelProgress = 0.0f;
        currentWaypointIndex = (currentWaypointIndex + 1) % waypoints.length;
    }

    Vec3 start = waypoints[currentWaypointIndex];
    Vec3 end = waypoints[(currentWaypointIndex + 1) % waypoints.length];

    float x = start.x + (end.x - start.x) * travelProgress;
    float z = start.z + (end.z - start.z) * travelProgress;
    float baseY = start.y + (end.y - start.y) * travelProgress;
    float wobble = (float)Math.sin(elapsedTime * 2.0) * 0.5f; 
    float y = baseY + wobble;

    bee.setPosition(x, y, z);
  }

  private void checkProximity() {
    Vec3 beePos = bee.getPosition();
    float threshold = 2.5f; 

    updateStatueTexture(statue1, statue1Pos, beePos, "cloud", threshold);
    updateStatueTexture(statue2, statue2Pos, beePos, "matrix", threshold);
    updateStatueTexture(statue3, statue3Pos, beePos, "dog", threshold);
  }

  private void updateStatueTexture(Model statue, Vec3 statuePos, Vec3 beePos, String originalTex, float threshold) {
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

  private Model makeSphere(GL3 gl, Mat4 m, String fragmentPath, Texture diffuse, Texture specular, Texture emission, Material mat) {
    String name = "sphere";
    Mesh mesh = new Mesh(gl, Sphere.vertices.clone(), Sphere.indices.clone());
    Mat4 modelMatrix = m;
    Shader shader = new Shader(gl, "assets/shaders/vs_standard.txt", fragmentPath);
    
    // Use the passed material, but set the texture maps
    Material material = mat.clone();
    material.setDiffuseMap(diffuse);
    material.setSpecularMap(specular);
    material.setEmissionMap(emission);
    
    Renderer renderer = new Renderer();
    return new Model(name, mesh, modelMatrix, shader, material, renderer, light, camera);
  }

  // Toggle Methods
  public void toggleGlobalLight() {
      lightOn = !lightOn;
  }
  
  public void toggleSpotlight() {
      spotOn = !spotOn;
  }
  
  public void toggleAnimationMode() {
      animationMode = !animationMode;
      // If switching to pose mode, maybe reset bee?
      if (!animationMode) {
          bee.setPosition(0, 5, 0); // Center
      }
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