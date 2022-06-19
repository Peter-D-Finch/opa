package com.dataxign.mark.aasruckmarchpacer;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Point;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup.LayoutParams;

import com.dataxign.mark.aasruckmarchpacer.geo.LocationPoint;
import com.dataxign.mark.aasruckmarchpacer.geo.Route;
import com.dataxign.mark.aasruckmarchpacer.geo.Segment;

public class MapChart_CustomView extends View {

	public int displayMode=0;
	public static final int MAP_DRAW_MAP=0;
	public static final int MAP_DRAW_GUIDE=1;

	public static final int SPEED_FASTER=1;
	public static final int SPEED_OK=0;
	public static final int SPEED_SLOWER=-1;
	public int speedGraphic=0;
	//public boolean useFtCampbellMap=false;

	Resources res;
	Bitmap base,ftcampbellmap;
	Paint textPaint, paint, paintLine, paintS,paintH;
	Typeface tf;

	int height;
	int width;

	private Route route=null;
	private LocationPoint currentLocation=null;
	public Segment currentSeg=null;
	public LocationPoint snap=null;

	double originalImageWidth = 800;
	double originalImageHeight = 800;
	double scaleWidthPercent = 1;
	double scaleHeightPercent = 1;
	double[] dataY = null;
	float numberOfYPixels = 125;
	float numberOfXpixels = 400;
	float yMin = 60;
	float yMax = 185;
	float range = yMax - yMin;
	float yPixelsPerUnit = numberOfYPixels / range;
	String[] yTickLabels = { "60", "85", "110", "135", "160", "185" };
	String[] xTickLabels = { "00:00", "00:00", "00:00", "00:00", "00:00" };

	//Map scaling parameters
	double pxperm_x;
	double pxperm_y;
	double xoff;
	double yoff;
	double ytop;


	public MapChart_CustomView(Context context) {
		super(context);
		commonConstruction(context);
	}

	public MapChart_CustomView(Context context, AttributeSet attrs) {
		super(context, attrs);
		commonConstruction(context);
	}

	public void commonConstruction(Context context) {
		res = context.getResources();
		base = BitmapFactory.decodeResource(res, R.drawable.gray_map_background800x800);
		ftcampbellmap=BitmapFactory.decodeResource(res, R.drawable.ruck_march_topo_cropped_800x800);

		paint = new Paint(Paint.ANTI_ALIAS_FLAG);

		paint.setStrokeWidth(3);
		paint.setColor(Color.YELLOW);
		// paint.setStyle(Paint.Style.FILL_AND_STROKE);
		paint.setAntiAlias(true);

		paintS = new Paint(Paint.ANTI_ALIAS_FLAG);
		paintS.setStrokeWidth(3);
		paintS.setColor(Color.CYAN);
		paintS.setAntiAlias(true);

		paintH = new Paint(Paint.ANTI_ALIAS_FLAG);
		paintH.setStrokeWidth(3);
		paintH.setColor(Color.GREEN);
		paintH.setAntiAlias(true);


		paintLine = new Paint(Paint.ANTI_ALIAS_FLAG);
		paintLine.setStrokeWidth(3);
		paintLine.setColor(Color.RED);
		// paint.setStyle(Paint.Style.FILL_AND_STROKE);
		paintLine.setAntiAlias(true);

		textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
		tf = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL);
		tf.isBold();
		textPaint.setTypeface(tf);
		textPaint.setStrokeWidth(2);
		textPaint.setColor(android.graphics.Color.BLACK);
		// textPaint.setTextScaleX(2.0f);
		textPaint.setTextSize(15);

		height = base.getHeight();
		width = base.getWidth();

		setLayoutParams(new LayoutParams(width, height));
		scaleWidthPercent = width / originalImageWidth;
		scaleHeightPercent = height / originalImageHeight;
	}

	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
		setMeasuredDimension(measureWidth(widthMeasureSpec),
				measureHeight(heightMeasureSpec));
	}

	private int measureWidth(int widthSpec) {
		return width;
	}

	private int measureHeight(int heightSpec) {
		return height;
	}

	@Override
	public void onDraw(Canvas canvas) {
		super.onDraw(canvas);

		Log.i("Map", "Drawing Map...");
		canvas.drawBitmap(base, 0, 0, null);

		if(displayMode==MAP_DRAW_MAP) {
			drawRoute(canvas);
			drawCurrentSeg(canvas);
			drawPoint(canvas);
			drawSnap(canvas);
			//drawAxes(canvas);
			//if (dataY != null)
			//plotChart(canvas);
			// addText(canvas);
		}
		else{
			drawGuidance(canvas);
		}

	}

	public void drawGuidance(Canvas c) {
		Paint paint2 = new Paint(Paint.ANTI_ALIAS_FLAG);
		paint2.setStrokeWidth(2);
		paint2.setColor(Color.BLACK);
		paint2.setStyle(Paint.Style.FILL_AND_STROKE);
		paint2.setAntiAlias(true);

		c.drawRect(0,0,800,800,paint2);

		Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
		paint.setStrokeWidth(2);
		paint.setColor(Color.YELLOW);
		paint.setStyle(Paint.Style.FILL_AND_STROKE);
		paint.setAntiAlias(true);

		if (speedGraphic == SPEED_FASTER) drawSpeedFaster(c, paint);
		if (speedGraphic == SPEED_SLOWER) drawSpeedSlower(c, paint);
		if (speedGraphic == SPEED_OK) drawSpeedOK(c, paint);
	}

	public void drawSpeedFaster(Canvas c, Paint p){


		Point point1_draw = new Point(0,600);
		Point point2_draw = new Point(400,0);
		Point point3_draw = new Point(800,600);

		Path path = new Path();
		path.setFillType(Path.FillType.EVEN_ODD);
		path.moveTo(point1_draw.x, point1_draw.y);
		path.lineTo(point2_draw.x, point2_draw.y);
		path.lineTo(point3_draw.x, point3_draw.y);
		path.lineTo(point1_draw.x, point1_draw.y);
		path.close();

		c.drawPath(path, p);

	}
	public void drawSpeedSlower(Canvas c, Paint p){
		Point point1_draw = new Point(0,200);
		Point point2_draw = new Point(400,800);
		Point point3_draw = new Point(800,200);

		Path path = new Path();
		path.setFillType(Path.FillType.EVEN_ODD);
		path.moveTo(point1_draw.x,point1_draw.y);
		path.lineTo(point2_draw.x,point2_draw.y);
		path.lineTo(point3_draw.x,point3_draw.y);
		path.lineTo(point1_draw.x,point1_draw.y);
		path.close();

		c.drawPath(path, p);
	}
	public void drawSpeedOK(Canvas c, Paint p){

		c.drawRect(0,250,800,550,p);

	}
	public void updateRoute(Route r){
		route=r;
	}

	public void drawCurrentSeg(Canvas c){
		Log.i("Map","Current Segment...");
		if (currentSeg==null)return;
				//draw this segment
		c.drawLine((float)(currentSeg.start.easting-xoff)*(float)(pxperm_x),(float)(ytop-(currentSeg.start.northing-yoff))*(float)(pxperm_y),(float)(currentSeg.end.easting-xoff)*(float)(pxperm_x),(float)(ytop-(currentSeg.end.northing-yoff))*(float)(pxperm_y),paintH);

	}

	public void drawRoute(Canvas c){
		Log.i("Map","Drawing Route...");
		if (route==null)return;
		if(route.title.equals("Ft. Campbell")){
			//Add Map
			c.drawBitmap(ftcampbellmap, 0, 0, null);

			//Map has it's own parameter that fix the offsets
			//LocationPoint br=new LocationPoint(36.607957,-87.449653); //ok
			LocationPoint br=new LocationPoint(36.608191,-87.449555); //pretty good

			LocationPoint tl=new LocationPoint(36.636063,-87.484535);//pretty good

			double rwidth=Math.abs(tl.easting-br.easting);
			double rheight=Math.abs(tl.northing-br.northing);
			//Compute pixels per meter
			pxperm_x = originalImageWidth / rwidth;
			pxperm_y = originalImageHeight / rheight;

			//if (pxperm_x < pxperm_y) pxperm_y = pxperm_x;
			//else pxperm_x = pxperm_y;

			//there is a valid route to draw
			xoff = tl.easting;
			yoff = tl.northing - rheight;
			ytop = rheight;

			Log.i("Map", "pxperm_x: " + pxperm_x + " pxperm_y: " + pxperm_y + " xoff: " + xoff + " yoff: " + yoff);
		}
		else {
			Log.i("Map", "Route: " + route.title + " Defined? " + route.routeDefined);
			if (route.routeDefined) {
				//Compute pixels per meter
				pxperm_x = originalImageWidth / route.width;
				pxperm_y = originalImageHeight / route.height;

				if (pxperm_x < pxperm_y) pxperm_y = pxperm_x;
				else pxperm_x = pxperm_y;

				//there is a valid route to draw
				xoff = route.left;
				yoff = route.top - route.height;
				ytop = route.height;

				Log.i("Map", "pxperm_x: " + pxperm_x + " pxperm_y: " + pxperm_y + " xoff: " + xoff + " yoff: " + yoff);
			}
		}

		for(int s=0;s<route.route.size();s++){
			Segment seg=route.route.get(s);
			//draw this segment
			c.drawLine((float)(seg.start.easting-xoff)*(float)(pxperm_x),(float)(ytop-(seg.start.northing-yoff))*(float)(pxperm_y),(float)(seg.end.easting-xoff)*(float)(pxperm_x),(float)(ytop-(seg.end.northing-yoff))*(float)(pxperm_y),paintLine);
		}

	}

	public void drawPoint(Canvas c){
		//draw the current point
		if(currentLocation==null)return;
		if(currentLocation.easting>0 && route.routeDefined){
			//Likely we have a point to plot
			c.drawCircle((float) (currentLocation.easting - xoff) * (float) (pxperm_x), (float) (ytop - (currentLocation.northing - yoff)) * (float) (pxperm_y), 10, paint);

		}
	}
	public void drawSnap(Canvas c){
		//draw the current point
		if(snap==null)return;
		if(snap.easting>0){
			//Likely we have a point to plot
			c.drawCircle((float)(snap.easting-xoff)*(float)(pxperm_x),(float)(ytop-(snap.northing-yoff))*(float)(pxperm_y),10,paintS);

		}
	}

	public void setLocation(LocationPoint p){
		currentLocation=p;
	}

	public void drawAxes(Canvas canvas) {
		float sx = (float) scaleWidthPercent;
		float sy = (float) scaleHeightPercent;
		float yOffset = 50f;
		float xOffset = 150f;

		// Y Axis
		canvas.drawLine(50 * sx, 10 * sy, 50 * sx, 150 * sy, paint);
		// Ticks Labels
		int yLabelCount = 0;
		float xyLabelOffSet = 15;
		float yyLabelOffSet = 5;
		for (float loc = 150; loc >= 25; loc = loc - 25) {
			canvas.drawLine(45 * sx, loc * sy, 50 * sx, loc * sy, paint);
			canvas.drawText(yTickLabels[yLabelCount], (xyLabelOffSet) * sx,
					(yyLabelOffSet + loc) * sy, textPaint);
			yLabelCount++;
		}
		// Tick Labels

		// X Axis
		canvas.drawLine(50 * sx, 150 * sy, 465 * sx, 150 * sy, paint);
		// Ticks
		int xLabelCount = 0;
		float xxLabelOffSet = -18;
		float yxLabelOffSet = 15;
		for (float loc = 50; loc <= 450; loc = loc + 100) {
			canvas.drawLine(loc * sx, 150 * sy, loc * sx, 155 * sy, paint);
			canvas.drawText(xTickLabels[xLabelCount], (xxLabelOffSet + loc)
					* sx, (155 + yxLabelOffSet) * sy, textPaint);
			xLabelCount++;
		}

	}

	public void plotChart(Canvas canvas) {
		float sx = (float) scaleWidthPercent;
		float sy = (float) scaleHeightPercent;
		float numPoints = dataY.length;
		if (numPoints < 2)
			return;
		int numPointsPerPixel=(int)Math.ceil(numPoints/numberOfXpixels);
		
		float numXPixelsPerUnit = numberOfXpixels / numPoints;
		float yBase = 150;
		float xBase = 50;
		// canvas.drawLine(50*sx, 150*sy, 100*sx, 100*sy, paintLine);
		for (int pt = numPointsPerPixel; pt < numPoints; pt=pt+numPointsPerPixel) {
			float x1 = (xBase + ((pt - numPointsPerPixel) * numXPixelsPerUnit)) * sx;
			float y1 = (yBase - ((float) dataY[pt - numPointsPerPixel] - yMin) * yPixelsPerUnit)
					* sy;
			float x2 = (xBase + ((pt) * numXPixelsPerUnit)) * sx;
			float y2 = (yBase - ((float) dataY[pt] - yMin) * yPixelsPerUnit)
					* sy;

			canvas.drawLine(x1, y1, x2, y2, paintLine);
		}
	}

	public void addDataY(double[] dY) {
		dataY = dY;
	}

	/**
	 * 
	 * @param labels
	 *            Array of 5 labels.
	 *
	 */
	public void addDataLabels(String[] labels, float min, float max) {
		yMin = min;
		yMax = max;
		range = max - min;
		yPixelsPerUnit = numberOfYPixels / range;
		yTickLabels = labels;
	}

	public void addXAxisLabels(long sessionTimeInMillis) {
		long quartiles = (long) ((float) sessionTimeInMillis / 4.0f);
		//xTickLabels[1] = Common.formatTimeCompact(quartiles);
		//xTickLabels[2] = Common.formatTimeCompact(quartiles * 2);
		//xTickLabels[3] = Common.formatTimeCompact(quartiles * 3);
		//xTickLabels[4] = Common.formatTimeCompact(sessionTimeInMillis);
	}

}
