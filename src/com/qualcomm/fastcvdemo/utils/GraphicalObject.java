package com.qualcomm.fastcvdemo.utils;

import java.util.Iterator;
import java.util.LinkedList;

import android.util.Log;

public class GraphicalObject {
	public static int OBJECT_RANGE = 38; // (WAS 50)  Number of pixels away corners can be to be considered part of the same object.
	public static int MINIMUM_SIZE = 25; // Must have this many corners to be targetable.
	public static int XY_RATIO_RANGE = 35; // (WAS 10) How close the pixel width and height must be to be considered a square.
	public static int SIDE_CHECK = 15; // Max distance from extreme to be considered part of the sides.
	public static double SIDE_RATIO = .75; // Ratio of points on side of object to be considered a square.
	public static int CUTOUT_RADIUS = 5; // (WAS 10) Radius from center in which no points may be found.
	public static int CUTOUT_WIDTH = 4; // Width from center lines in which no points may be found.
	
	
	public static class Point {
		public final int x;
		public final int y;

		public Point(int x, int y) {
			
			this.x = x;
			this.y = y;
		}
		
		public double distanceFrom(Point p) {
			return Math.sqrt(Math.pow(y - p.y, 2) + Math.pow(x - p.x, 2));
		}
	}
	
	private LinkedList<Point> corners;
	private int[][] range = {{Integer.MAX_VALUE, Integer.MIN_VALUE}, {Integer.MAX_VALUE, Integer.MIN_VALUE}}; //[[x min, x max], [y min, y max]]

	private GraphicalObject(LinkedList<Point> corners) {
		this.corners = corners;

		// Assuming the target square is roughly head on and not rotated, this is
		// both faster and more accurate than finding the average point.
		for (Point point : corners) {
			range[0][0] = Math.min(range[0][0], point.x);
			range[0][1] = Math.max(range[0][1], point.x);
			range[1][0] = Math.min(range[1][0], point.y);
			range[1][1] = Math.max(range[1][1], point.y);
		}
		
	}
	
	
	public int[][] getRange() {
		return range;
	}
	

	
	// Need enough corners to consider shooting.
	public boolean isBigEnough() {
		return corners.size() > MINIMUM_SIZE; 
	}


	//Enough of the corners must be close to the extremes, and the x range must be close to the y range.
	public boolean isSquare() {
		int count = 0;
		
		if (Math.abs((range[0][0] - range[0][1]) -
					 (range[1][0] - range[1][1])) > XY_RATIO_RANGE)	//Rectangular extremes are skewed.
			return false;
		
		for (Point pnt : corners) {
			if (Math.abs(pnt.x - range[0][0]) < SIDE_CHECK ||
				Math.abs(pnt.x - range[0][1]) < SIDE_CHECK ||
				Math.abs(pnt.y - range[1][0]) < SIDE_CHECK ||
				Math.abs(pnt.y - range[1][1]) < SIDE_CHECK)
				count++;	// Point is near a rectangular extreme.
		}
		
		return count >= corners.size() * SIDE_RATIO;
	}

	
	//Must have no corners within 5 pixels of the center or within 2 pixels of the x and y axis.
	public boolean hasCutout() {
		Point center = getCenter();
		
		for (Point pnt : corners) {
			if (pnt.distanceFrom(center) < CUTOUT_RADIUS)
				return false;
			if (Math.abs(pnt.x - center.x) < CUTOUT_WIDTH ||
				Math.abs(pnt.y - center.y) < CUTOUT_WIDTH)
				return false;
		}
		
		return true;
	}
	
	
	public Point getCenter() {
		// Center point is the average of the extremes.
		return new Point((range[0][0] + range[0][1]) / 2, (range[1][0] + range[1][1]) / 2);
	}
	
	

	public static LinkedList<GraphicalObject> getObjectsFromCorners(int[] corners) {
		LinkedList<GraphicalObject> output = new LinkedList<GraphicalObject>();
		LinkedList<Point> points = new LinkedList<Point>();
		
		
		//Convert corner array to point array.
		for (int i = 1; i < corners.length; i += 2) {
			points.add(new Point(corners[i-1], corners[i]));
		}
		
		
		//While we have more points not ascosiated with an object, get another object from the first point.
		while (points.size() > 0) {						//Remove each point to avoid duplicates in an object.
			output.add(new GraphicalObject(getPointField(points, points.poll())));
		}
		
		return output;
	}
	
	private static LinkedList<Point> getPointField(LinkedList<Point> corners, Point localPoint) {
		// List of points in this object.
		LinkedList<Point> points = new LinkedList<Point>();
		
		// Use iterator for improved performance with remove() function.
		Iterator<Point> iter = corners.iterator();
		while (iter.hasNext()) {
			Point corner = iter.next();
			if (corner.distanceFrom(localPoint) < OBJECT_RANGE) {
				points.add(corner);
				iter.remove();	//Remove so we don't find this point again. (Note: Changes original list for calling function)
			}
		}


		// Add in all the points that are connected to points we just found.
		for (Point point : points.toArray(new Point[points.size()])) {
			points.addAll(getPointField(corners, point));
		}
		
		// Add in the point we started with and return.
		points.add(localPoint);
		return points;
	}
}
