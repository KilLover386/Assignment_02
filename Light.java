import gmaths.*;
import java.nio.*;
import com.jogamp.common.nio.*;
import com.jogamp.opengl.*;
  
public class Light {
  
  private Material material;
  private Vec3 position;
  private Mat4 modelMatrix;
  private Shader shader;
  private Camera camera;
  private Mesh mesh;
  
  // Spotlight properties
  private Vec3 direction;
  private float cutoff;
  private boolean on = true;
    
  public Light(GL3 gl, Camera camera) {
    this(gl, MaterialConstants.dullWhiteLightSource, new Vec3(3f,2f,1f), camera);
  }

  public Light(GL3 gl, Material material, Vec3 position, Camera camera) {
    this.material = material;
    this.position = position;
    this.camera = camera;
    
    // Default spotlight values (pointing down, 12.5 degree cone)
    this.direction = new Vec3(0,-1,0);
    this.cutoff = (float)Math.cos(Math.toRadians(12.5f)); 

    modelMatrix = new Mat4(1);
    modelMatrix = Mat4.multiply(Mat4Transform.scale(10f,10f,10f), modelMatrix);
    shader = new Shader(gl, "assets/shaders/vs_light_01.txt", "assets/shaders/fs_light_01.txt");
    mesh = new Mesh(gl, Sphere.vertices, Sphere.indices);
  }
  
  public void setPosition(Vec3 v) {
    position.x = v.x;
    position.y = v.y;
    position.z = v.z;
  }
  
  public void setPosition(float x, float y, float z) {
    position.x = x;
    position.y = y;
    position.z = z;
  }
  
  public Vec3 getPosition() {
    return position;
  }
  
  public void setMaterial(Material m) {
    material = m;
  }
  
  public Material getMaterial() {
    return material;
  }
  
  public void setCamera(Camera camera) {
    this.camera = camera;
  }

  public void setDirection(Vec3 v) {
    direction = new Vec3(v);
    direction.normalize();
  }

  public Vec3 getDirection() {
    return direction;
  }

  public void setCutoff(float c) {
    cutoff = c;
  }

  public float getCutoff() {
    return cutoff;
  }

  public void setOn(boolean b) {
    on = b;
  }

  public boolean isOn() {
    return on;
  }
  
  public void render(GL3 gl) { 
    Mat4 localMM = Mat4.multiply(Mat4Transform.translate(position), modelMatrix);
    Mat4 mvpMatrix = Mat4.multiply(camera.getPerspectiveMatrix(), 
                                   Mat4.multiply(camera.getViewMatrix(),
                                   localMM));
    shader.use(gl);
    shader.setVec3(gl, "colour", material.getDiffuse());
    shader.setFloatArray(gl, "mvpMatrix", mvpMatrix.toFloatArrayForGLSL());
    
    mesh.render(gl);
  }
}