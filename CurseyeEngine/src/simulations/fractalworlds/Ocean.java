package simulations.fractalworlds;

import engine.core.Constants;
import engine.math.Quaternion;
import engine.renderer.water.WaterSurface;

public class Ocean extends WaterSurface{

	public Ocean() {
		super(32);
		
		getTransform().setScaling(Constants.ZFAR,1,Constants.ZFAR);
		getTransform().setTranslation(-Constants.ZFAR/2,0,-Constants.ZFAR/2);
		
		setClipplane(new Quaternion(0,-1,0,getTransform().getTranslation().getY()));

		this.loadSettingsFile("./res/terrains/terrain0/waterSettings.ocn");
	}

}
