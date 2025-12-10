import gmaths.*;
import java.nio.*;
import com.jogamp.common.nio.*;
import com.jogamp.opengl.*;
import com.jogamp.opengl.util.*;
import com.jogamp.opengl.util.awt.*;
import com.jogamp.opengl.util.glsl.*;
import com.jogamp.opengl.util.texture.*;
  
public class main_GLEventListener implements GLEventListener {
  
  private Camera camera;
  
  // Toggles
  private boolean lightOn = true;
  private boolean spotLightOn = true;
  private boolean spotAnimOn = true;
  private boolean animationMode = true; 
    
  public main_GLEventListener(Camera camera) {
    this.camera = camera;
    this.camera.setPosition(new Vec3(4f,6f,15f));
    this.camera.setTarget(new Vec3(0f,0,0f));
  }
  
  public void init(GLAutoDrawable drawable) {
    GL3 gl = drawable.getGL().getGL3();
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
    textures.destroy(drawable.getGL().getGL3());
  }

  // ***************************************************
  // THE SCENE

  private Model statue1, statue2, statue3;
  // Positions
  private Vec3 statue1Pos = new Vec3(3, 0.5f, 0);
  private Vec3 statue2Pos = new Vec3(-3f, 0.5f, 3f);
  private Vec3 statue3Pos = new Vec3(-3f, 0.5f, -3f);

  private Floor floor;
  private Walls walls;
  private Light light;
  private Bee bee;
  
  private Light spotlightLight;
  private Spotlight spotlightObject;
  private TextureLibrary textures;

  // Animation Variables
  private Vec3[] waypoints;
  private int currentWaypointIndex = 0;
  private float travelProgress = 0.0f;
  private float travelSpeed = 0.5f;

  public void initialise(GL3 gl) {
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
    textures.add(gl, "metal", "assets/textures/metal.jpg");
    textures.add(gl, "metal2", "assets/textures/metal2.jpg");
    textures.add(gl, "stone", "assets/textures/stone.jpg");
    textures.add(gl, "stone2", "assets/textures/stone2.jpg");
    textures.add(gl, "wood", "assets/textures/wood.jpg");
    textures.add(gl, "wood2", "assets/textures/wood2.jpg");
    

    // Waypoints for continuous flight
    waypoints = new Vec3[] {
      new Vec3(statue1Pos.x + 2, 3, statue1Pos.z), // Near Statue 1
      new Vec3(0, 5, 0),                           // Center/High
      new Vec3(statue2Pos.x, 3, statue2Pos.z + 2), // Near Statue 2
      new Vec3(0, 2, -5),                          // Random spot
      new Vec3(statue3Pos.x + 2, 3, statue3Pos.z)  // Near Statue 3
    };

    // Materials
    Material mStone = new Material(new Vec3(0.1f, 0.1f, 0.1f), new Vec3(0.6f, 0.6f, 0.6f), new Vec3(0.1f, 0.1f, 0.1f), 4.0f);
    Material mMetal = new Material(new Vec3(0.2f, 0.2f, 0.2f), new Vec3(0.7f, 0.7f, 0.7f), new Vec3(1.0f, 1.0f, 1.0f), 64.0f);
    Material mWood = new Material(new Vec3(0.1f, 0.1f, 0.1f), new Vec3(0.6f, 0.4f, 0.2f), new Vec3(0.2f, 0.2f, 0.2f), 8.0f);

    // Light
    light = new Light(gl, camera);
    light.setPosition(0f, 25f, 0f);           

    // Spotlight
    spotlightLight = new Light(gl, camera);
    Material spotMat = new Material();
    spotMat.setAmbient(0.0f, 0.0f, 0.0f); 
    spotMat.setDiffuse(1.0f, 1.0f, 1.0f); 
    spotMat.setSpecular(1.0f, 1.0f, 1.0f);
    spotlightLight.setMaterial(spotMat);
    spotlightLight.setCutoff((float)Math.cos(Math.toRadians(15.0))); 
    Renderer.spotlight = spotlightLight;

    // Objects
    floor = new Floor(gl, light, camera, textures.get("snow_ground"));
    walls = new Walls(gl, light, camera, textures);
    bee = new Bee(gl, light, camera, textures);
    spotlightObject = new Spotlight(gl, spotlightLight, camera, textures);

    // Statues (Spheres)
    Mat4 mSphere = Mat4Transform.translate(statue1Pos);
    mSphere = Mat4.multiply(Mat4Transform.scale(2,4,2),mSphere);
    statue1 = makeSphere(gl, mSphere, "assets/shaders/fs_standard_d.txt", textures.get("metal"), null, null, mStone);

    Mat4 mSphere2 = Mat4Transform.translate(statue2Pos);
    mSphere2 = Mat4.multiply(Mat4Transform.scale(1.5f,3f,1.5f),mSphere2);
    statue2 = makeSphere(gl, mSphere2, "assets/shaders/fs_standard_d.txt", textures.get("stone"), null, null, mMetal);

    Mat4 mSphere3 = Mat4Transform.translate(statue3Pos);
    mSphere3 = Mat4.multiply(Mat4Transform.scale(2f,4f,2f),mSphere3);
    statue3 = makeSphere(gl, mSphere3, "assets/shaders/fs_standard_d.txt", textures.get("wood"), null, null, mWood);
  
    bee.setPosition(waypoints[0].x, waypoints[0].y, waypoints[0].z);              
  }

  public void render(GL3 gl) {
    gl.glClear(GL.GL_COLOR_BUFFER_BIT | GL.GL_DEPTH_BUFFER_BIT);
    
    // Updates
    if (spotAnimOn) {
        spotlightObject.update(); 
    }
    
    if (animationMode) updateBeeMovement();
    checkProximity();

    // Tell the light if the Bulb is ON
    spotlightLight.setOn(spotLightOn);

    // Light Handling [FIX]
    if (!lightOn) {
        // Dim the scene (Moonlight effect) instead of full black
        light.getMaterial().setDiffuse(0.2f, 0.2f, 0.2f); 
        light.getMaterial().setSpecular(0.2f, 0.2f, 0.2f);
        light.getMaterial().setAmbient(0.05f, 0.05f, 0.05f);
    } else {
        // Full daylight
        light.getMaterial().setAmbient(0.1f, 0.1f, 0.1f);
        light.getMaterial().setDiffuse(0.6f, 0.6f, 0.6f);
        light.getMaterial().setSpecular(0.6f, 0.6f, 0.6f);
    }
    
    //spotlightLight.setOn(spotOn);

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
    float wobble = (float)Math.sin(elapsedTime * 4.0) * 0.5f; 
    float y = baseY + wobble;

    bee.setPosition(x, y, z);
  }

  public void jumpToPose(int index) {
      if (animationMode) return;
      
      Vec3 target = new Vec3(0,0,0);
      switch(index) {
          case 0: target = new Vec3(statue1Pos.x + 2.5f, 2, statue1Pos.z); break; 
          case 1: target = new Vec3(statue2Pos.x, 2, statue2Pos.z + 2.5f); break; 
          case 2: target = new Vec3(statue3Pos.x + 2.5f, 2, statue3Pos.z); break; 
      }
      bee.setPosition(target.x, target.y, target.z);
      bee.setRotationAngle(90f); // Ensure it stays horizontal
  }

  private void checkProximity() {
    Vec3 beePos = bee.getPosition();
    float threshold = 3.0f; 

    boolean near1 = updateStatueTexture(statue1, statue1Pos, beePos, "metal", threshold);
    boolean near2 = updateStatueTexture(statue2, statue2Pos, beePos, "stone", threshold);
    boolean near3 = updateStatueTexture(statue3, statue3Pos, beePos, "wood", threshold);

    bee.reactToProximity(near1 || near2 || near3);
  }

  private boolean updateStatueTexture(Model statue, Vec3 statuePos, Vec3 beePos, String originalTex, float threshold) {
    float dx = statuePos.x - beePos.x;
    float dy = statuePos.y - beePos.y;
    float dz = statuePos.z - beePos.z;
    float dist = (float)Math.sqrt(dx*dx + dy*dy + dz*dz);

    if (dist < threshold) {
      if (originalTex.equals("metal")) { statue.getMaterial().setDiffuseMap(textures.get("metal2")); }
      else if (originalTex.equals("stone")) { statue.getMaterial().setDiffuseMap(textures.get("stone2")); }
      else if (originalTex.equals("wood")) {statue.getMaterial().setDiffuseMap(textures.get("wood2"));}
      return true;
    } else {
      statue.getMaterial().setDiffuseMap(textures.get(originalTex));
      return false;
    }
  }

  private Model makeSphere(GL3 gl, Mat4 m, String fragmentPath, Texture diffuse, Texture specular, Texture emission, Material mat) {
    String name = "sphere";
    Mesh mesh = new Mesh(gl, Sphere.vertices.clone(), Sphere.indices.clone());
    Mat4 modelMatrix = m;
    Shader shader = new Shader(gl, "assets/shaders/vs_standard.txt", fragmentPath);
    Material material = mat.clone();
    material.setDiffuseMap(diffuse);
    material.setSpecularMap(specular);
    material.setEmissionMap(emission);
    Renderer renderer = new Renderer();
    return new Model(name, mesh, modelMatrix, shader, material, renderer, light, camera);
  }

  public void toggleGlobalLight() { lightOn = !lightOn; }
  public void toggleSpotLight()   { spotLightOn = !spotLightOn; } // Bulb
  public void toggleSpotAnim()    { spotAnimOn = !spotAnimOn; }   // Motor
  public void toggleAnimationMode() { animationMode = !animationMode; }

  private double startTime;
  private double getSeconds() { return System.currentTimeMillis()/1000.0; }
}