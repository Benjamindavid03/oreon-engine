package engine.core;

import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.Display;

import engine.main.CoreEngine;
import engine.math.Matrix4f;
import engine.math.Quaternion;
import engine.math.Vec2f;
import engine.math.Vec3f;

public class Camera {
	
	private static Camera instance = null;

	private final Vec3f yAxis = new Vec3f(0,1,0);
	
	private Vec3f position;
	private Vec3f forward;
	private Vec3f up;
	private int scaleFactor;
	private Matrix4f viewMatrix;
	private Matrix4f projectionMatrix;
	private Matrix4f viewProjectionMatrix;
	private Matrix4f previousViewMatrix;
	private Matrix4f previousViewProjectionMatrix;
	
	private float zNear;
	private float zFar;
	private float width;
	private float height;
	private float fovY;
	
	private boolean mouselocked;
	private Quaternion[] frustumPlanes = new Quaternion[6];
	private Vec2f lockedMousePosition;
	private Vec2f currentMousePosition;
	private float rotYstride;
	private float rotYamt;
	private float rotYcounter;
	private boolean rotYInitiated = false;
	private float rotXstride;
	private float rotXamt;
	private float rotXcounter;
	private boolean rotXInitiated = false;
	private float mouseSensitivity = 0.2f;
	   
	public static Camera getInstance() 
	{
	    if(instance == null) 
	    {
	    	instance = new Camera();
	    }
	      return instance;
	}
	
	protected Camera()
	{
		this(new Vec3f(0,200,-2500), new Vec3f(0,0,1), new Vec3f(0,1,0));
		this.setProjection(54, Window.getWidth(), Window.getHeight(), Constants.ZNEAR, Constants.ZFAR);
		this.projectionMatrix = new Matrix4f().Projection(fovY, width, height, zNear, zFar);
		this.setViewMatrix(new Matrix4f().View(this.getForward(), this.getUp()).mul(
				new Matrix4f().Translation(this.getPosition().mul(-1))));
		this.initfrustumPlanes();
		this.viewProjectionMatrix = new Matrix4f().Zero();
		this.previousViewProjectionMatrix = new Matrix4f().Zero();
	}
	
	private Camera(Vec3f position, Vec3f forward, Vec3f up)
	{
		this.setPosition(position);
		this.setForward(forward);
		this.setUp(up);
		this.setScaleFactor(100);
		up.normalize();
		forward.normalize();
	}
	
	public void input()
	{
		this.setScaleFactor(Math.max(10, scaleFactor + Mouse.getDWheel()/12));
		
		float movAmt = scaleFactor/4.0f *  CoreEngine.getFrameTime();
		float rotAmt = scaleFactor* 0.1f * CoreEngine.getFrameTime(); 
		
		if(Input.getButtonDown(2))
		{
			Input.setCursor(false);
			lockedMousePosition = Input.getMousePos();
			mouselocked = true;
		}
		
		if(Input.getButtonreleased(2))
		{
			Input.setCursor(true);
			mouselocked = false;
		}	
		
		if(Input.getHoldingKey(Keyboard.KEY_W))
			move(getForward(), movAmt);
		if(Input.getHoldingKey(Keyboard.KEY_S))
			move(getForward(), -movAmt);
		if(Input.getHoldingKey(Keyboard.KEY_A))
			move(getLeft(), movAmt);
		if(Input.getHoldingKey(Keyboard.KEY_D))
			move(getRight(), movAmt);
		
		if(Input.getHoldingKey(Keyboard.KEY_UP))
			rotateX(-rotAmt);
		if(Input.getHoldingKey(Keyboard.KEY_DOWN))
			rotateX(rotAmt);
		if(Input.getHoldingKey(Keyboard.KEY_LEFT))
			rotateY(-rotAmt);
		if(Input.getHoldingKey(Keyboard.KEY_RIGHT))
			rotateY(rotAmt);
		
		// free mouse rotation
		if(mouselocked)
		{
			currentMousePosition = Input.getMousePos();
			
			float dy = lockedMousePosition.getY() - currentMousePosition.getY();
			float dx = lockedMousePosition.getX() - currentMousePosition.getX();
			
			// y-axxis rotation
			
			if (dy != 0){
				rotYstride = Math.abs(dy * CoreEngine.getFrameTime() * 10);
				rotYamt = dy;
				rotYcounter = 0;
				rotYInitiated = true;
			}
			
			if (rotYInitiated){
				
				// up-rotation
				if (rotYamt < 0){
					if (rotYcounter > rotYamt){
						rotateX(-rotYstride * mouseSensitivity);
						rotYcounter -= rotYstride;
					}
					else rotYInitiated = false;
				}
				// down-rotation
				else if (rotYamt > 0){
					if (rotYcounter < rotYamt){
						rotateX(rotYstride * mouseSensitivity);
						rotYcounter += rotYstride;
					}
					else rotYInitiated = false;
				}
			}
			
			// x-axxis rotation
			if (dx != 0){
				rotXstride = Math.abs(dx * CoreEngine.getFrameTime() * 10);
				rotXamt = dx;
				rotXcounter = 0;
				rotXInitiated = true;
			}
			
			if (rotXInitiated){
				
				// up-rotation
				if (rotXamt < 0){
					if (rotXcounter > rotXamt){
						rotateY(rotXstride * mouseSensitivity);
						rotXcounter -= rotXstride;
					}
					else rotXInitiated = false;
				}
				// down-rotation
				else if (rotXamt > 0){
					if (rotXcounter < rotXamt){
						rotateY(-rotXstride * mouseSensitivity);
						rotXcounter += rotXstride;
					}
					else rotXInitiated = false;
				}
			}
		}
		
		if(mouselocked) Input.setMousePosition(lockedMousePosition);
		
		setPreviousViewMatrix(viewMatrix);
		setPreviousViewProjectionMatrix(viewProjectionMatrix);
		setViewMatrix(new Matrix4f().View(this.getForward(), this.getUp()).mul(
				new Matrix4f().Translation(this.getPosition().mul(-1))));
		setViewProjectionMatrix(projectionMatrix.mul(viewMatrix));
		
//		System.out.println(position);
//		System.out.println(previousPosition);
//		System.out.println(forward);
//		System.out.println(up);
	}
	
	public void move(Vec3f dir, float amount)
	{
		Vec3f newPos = position.add(dir.mul(amount));	
		setPosition(newPos);
	}
	
	private void initfrustumPlanes()
	{
		// ax * bx * cx +  d = 0; store a,b,c,d
		
		//left plane
		Quaternion leftPlane = new Quaternion(
				this.projectionMatrix.get(3, 0) + this.projectionMatrix.get(0, 0) * (float) ((Math.tan(Math.toRadians(this.fovY/2)) * ((double) Display.getWidth()/ (double) Display.getHeight()))),
				this.projectionMatrix.get(3, 1) + this.projectionMatrix.get(0, 1),
				this.projectionMatrix.get(3, 2) + this.projectionMatrix.get(0, 2),
				this.projectionMatrix.get(3, 3) + this.projectionMatrix.get(0, 3));
		
				this.frustumPlanes[0] = Util.normalizePlane(leftPlane);
		
		//right plane
		Quaternion rightPlane = new Quaternion(
				this.projectionMatrix.get(3, 0) - this.projectionMatrix.get(0, 0) * (float) ((Math.tan(Math.toRadians(this.fovY/2)) * ((double) Display.getWidth()/ (double) Display.getHeight()))),
				this.projectionMatrix.get(3, 1) - this.projectionMatrix.get(0, 1),
				this.projectionMatrix.get(3, 2) - this.projectionMatrix.get(0, 2),
				this.projectionMatrix.get(3, 3) - this.projectionMatrix.get(0, 3));
		
				this.frustumPlanes[1] = Util.normalizePlane(rightPlane);
		
		//bot plane
		Quaternion botPlane = new Quaternion(
				this.projectionMatrix.get(3, 0) + this.projectionMatrix.get(1, 0),
				this.projectionMatrix.get(3, 1) + this.projectionMatrix.get(1, 1) * (float) Math.tan(Math.toRadians(this.fovY/2)),
				this.projectionMatrix.get(3, 2) + this.projectionMatrix.get(1, 2),
				this.projectionMatrix.get(3, 3) + this.projectionMatrix.get(1, 3));
		
				this.frustumPlanes[2] = Util.normalizePlane(botPlane);
		
		//top plane
		Quaternion topPlane = new Quaternion(
				this.projectionMatrix.get(3, 0) - this.projectionMatrix.get(1, 0),
				this.projectionMatrix.get(3, 1) - this.projectionMatrix.get(1, 1) * (float) Math.tan(Math.toRadians(this.fovY/2)),
				this.projectionMatrix.get(3, 2) - this.projectionMatrix.get(1, 2),
				this.projectionMatrix.get(3, 3) - this.projectionMatrix.get(1, 3));
		
				this.frustumPlanes[3] = Util.normalizePlane(topPlane);
		
		//near plane
		Quaternion nearPlane = new Quaternion(
				this.projectionMatrix.get(3, 0) + this.projectionMatrix.get(2, 0),
				this.projectionMatrix.get(3, 1) + this.projectionMatrix.get(2, 1),
				this.projectionMatrix.get(3, 2) + this.projectionMatrix.get(2, 2),
				this.projectionMatrix.get(3, 3) + this.projectionMatrix.get(2, 3));
		
				this.frustumPlanes[4] = Util.normalizePlane(nearPlane);
		
		//far plane
				Quaternion farPlane = new Quaternion(
				this.projectionMatrix.get(3, 0) - this.projectionMatrix.get(2, 0),
				this.projectionMatrix.get(3, 1) - this.projectionMatrix.get(2, 1),
				this.projectionMatrix.get(3, 2) - this.projectionMatrix.get(2, 2),
				this.projectionMatrix.get(3, 3) - this.projectionMatrix.get(2, 3));
				
				this.frustumPlanes[5] = Util.normalizePlane(farPlane);
	}
	
	public void rotateY(float angle)
	{
		Vec3f hAxis = yAxis.cross(forward).normalize();
		
		forward.rotate(angle, yAxis).normalize();
		
		up = forward.cross(hAxis).normalize();
	}
	
	public void rotateX(float angle)
	{
		Vec3f hAxis = yAxis.cross(forward).normalize();

		forward.rotate(angle, hAxis).normalize();
		
		up = forward.cross(hAxis).normalize();
	}
	
	public Vec3f getLeft()
	{
		Vec3f left = forward.cross(up);
		left.normalize();
		return left;
	}
	
	public Vec3f getRight()
	{
		Vec3f right = up.cross(forward);
		right.normalize();
		return right;
	}

	public int getScaleFactor() {
		return this.scaleFactor;
	}

	public void setScaleFactor(int scaleFactor) {
		this.scaleFactor = scaleFactor;
	}

	public Matrix4f getProjectionMatrix() {
		return projectionMatrix;
	}

	public void setProjectionMatrix(Matrix4f projectionMatrix) {
		this.projectionMatrix = projectionMatrix;
	}
	
	public  void setProjection(float fovY, float width, float height, float zNear, float zFar)
	{
		this.fovY = fovY;
		this.width = width;
		this.height = height;
		this.zNear = zNear;
		this.zFar = zFar;
	}

	public Matrix4f getViewMatrix() {
		return viewMatrix;
	}

	public void setViewMatrix(Matrix4f viewMatrix) {
		this.viewMatrix = viewMatrix;
	}

	public Vec3f getPosition() {
		return position;
	}

	public void setPosition(Vec3f position) {
		this.position = position;
	}

	public Vec3f getForward() {
		return forward;
	}

	public void setForward(Vec3f forward) {
		this.forward = forward;
	}

	public Vec3f getUp() {
		return up;
	}

	public void setUp(Vec3f up) {
		this.up = up;
	}

	public Quaternion[] getFrustumPlanes() {
		return frustumPlanes;
	}
	
	public float getZFar()
	{
		return this.zFar;
	}

	public void setViewProjectionMatrix(Matrix4f viewProjectionMatrix) {
		this.viewProjectionMatrix = viewProjectionMatrix;
	}
	
	public Matrix4f getViewProjectionMatrix() {
		return viewProjectionMatrix;
	}

	public Matrix4f getPreviousViewProjectionMatrix() {
		return previousViewProjectionMatrix;
	}

	public void setPreviousViewProjectionMatrix(
			Matrix4f previousViewProjectionMatrix) {
		this.previousViewProjectionMatrix = previousViewProjectionMatrix;
	}

	public Matrix4f getPreviousViewMatrix() {
		return previousViewMatrix;
	}

	public void setPreviousViewMatrix(Matrix4f previousViewMatrix) {
		this.previousViewMatrix = previousViewMatrix;
	}
}
