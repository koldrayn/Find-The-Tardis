package data;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import javafx.scene.paint.Color;


public class GameCamera {
    GraphicsContext gc;
    int resolution;
    final int MAX_RESOLUTION = 500;
    final int MAX_VIEW_DISTANCE = 20;
    final double SHADING_DISTANCE = 3;
    final int MIN_RAINDROPS = 0;
    final int MAX_RAINDROPS = 1;

    double cosAngelAngel;


    PrPlane prPlane;

    private class PrPlane {
        double width;
        double height;
        double distance;
        double columnWidth;

        PrPlane(double width, double height, double FOV) {
            this.width = width;
            this.height = height;
            this.distance = (width / 2) / Math.tan(FOV / 2);

            columnWidth = width / resolution;

        }
    }

    static final double CIRCLE = Math.PI * 2;

    private double EPSILON = 0.0005;


    public GameCamera(GraphicsContext gc, int resolution, double fov) {
        this.resolution = (resolution > 5 && resolution < MAX_RESOLUTION) ? resolution : MAX_RESOLUTION;
        this.gc = gc;
        cosAngelAngel = Math.cos(fov / 1.5);
        prPlane = new PrPlane(gc.getCanvas().getWidth(), gc.getCanvas().getHeight(), fov);
    }

    class Ray {
        class RayPoint {
            double distance;
            double entry;

            RayPoint(double distance, double entry) {
                this.distance = distance;
                this.entry = entry;
            }
        }

        double angle;
        RayPoint[] rayPoints = new RayPoint[3];

        Ray(double angle, Maze maze, Maze.Coords startPoint) {
            this.angle = angle;
            cast(maze, startPoint, 0);
        }

        private double entry(Maze.Coords point, double angle, boolean isX) {
            if (angle < CIRCLE / 4) {
                if (isX)
                    return (Math.floor(point.y + 1) - point.y);
                else
                    return (point.x - Math.ceil(point.x - 1));
            } else if (angle < CIRCLE / 2) {
                if (isX)
                    return (point.y - Math.ceil(point.y - 1));
                else
                    return (point.x - Math.ceil(point.x - 1));
            } else if (angle < 3 * CIRCLE / 4) {
                if (isX)
                    return (point.y - Math.ceil(point.y - 1));
                else
                    return (Math.floor(point.x + 1) - point.x);
            } else {
                if (isX)
                    return (Math.floor(point.y + 1) - point.y);
                else
                    return (Math.floor(point.x + 1) - point.x);
            }
        }

        private void cast(Maze maze, Maze.Coords point, double distance) {
            double cos = Math.cos(angle);
            double sin = Math.sin(angle);

            int switcher;
            double newDist;
            Maze.Coords newPoint;
            boolean isX;

            if (Math.abs(cos) < EPSILON) {
                newPoint = new Maze.Coords(maze, point.x, sin > 0 ? Math.floor(point.y + 1) : Math.ceil(point.y - 1));
                switcher = maze.map[(int) Math.floor(newPoint.x)][(int) Math.floor(newPoint.y - (sin > 0 ? 0 : 1))];
                newDist = distance + Math.abs(point.y - newPoint.y);
                isX = false;
            } else if (Math.abs(sin) < EPSILON) {
                newPoint = new Maze.Coords(maze, cos > 0 ? Math./**/floor(point.x + 1) : Math./**/ceil(point.x - 1), point.y);
                switcher = maze.map[(int) Math.floor(newPoint.x - (cos > 0 ? 0 : 1))][(int) Math.floor(newPoint.y)];
                newDist = distance + Math.abs(point.x - newPoint.x);
                isX = true;
            } else {
                double stepX = cos > 0 ? Math.floor(point.x + 1) - point.x : Math.ceil(point.x - 1) - point.x;
                double stepY = sin > 0 ? Math.floor(point.y + 1) - point.y : Math.ceil(point.y - 1) - point.y;

                if (stepX / cos < stepY / sin) {
                    switcher = maze.map[(int) (cos > 0 ? Math.floor(point.x + 1) : Math.ceil(point.x - 2))][(int) (sin < 0 ? Math.ceil(point.y - 1) : Math.floor(point.y))];
                    newDist = distance + stepX / cos;
                    newPoint = new Maze.Coords(maze, point.x + stepX, point.y + stepX / cos * sin);
                    isX = true;
                } else {
                    switcher = maze.map[(int) (cos < 0 ? Math.ceil(point.x - 1) : Math.floor(point.x))][(int) (sin > 0 ? Math.floor(point.y + 1) : Math.ceil(point.y - 2))];
                    newDist = distance + stepY / sin;
                    newPoint = new Maze.Coords(maze, point.x + stepY / sin * cos, point.y + stepY);
                    isX = false;
                }
            }
            switch (switcher) {
                case Resources.Blocks.WALL:
                    rayPoints[Resources.Blocks.WALL] = new RayPoint(newDist, entry(newPoint, angle, isX));
                    break;
                case Resources.Blocks.TARDIS:
                    rayPoints[Resources.Blocks.TARDIS] = new RayPoint(newDist, entry(newPoint, angle, isX));
                default:
                    if (newDist < GameCamera.this.MAX_VIEW_DISTANCE)
                        cast(maze, newPoint, newDist);
                    break;
            }


        }


    }

    private void drawTexture(Ray.RayPoint rPoint, Image texture, int number, double angle, double blockHeight) {
        double distance = rPoint.distance * Math.cos(angle);

        double texture_startX = texture.getWidth() * rPoint.entry;
        double texture_startY = 0;
        double texture_width = texture.getWidth() / resolution;
        double texture_height = texture.getHeight();

        double startX = prPlane.columnWidth * number;
        double width = prPlane.columnWidth;

        double height = blockHeight * prPlane.height/*prPlane.distance*/ / distance;
        height += ((int) height) % 2;
        double startY = (prPlane.height / 2) * (1 + 1 / distance) - height;

        gc.drawImage(texture, texture_startX, texture_startY, texture_width, texture_height, startX, startY, width, height);

            gc.setGlobalAlpha(distance<SHADING_DISTANCE?distance/SHADING_DISTANCE:1.0);
            gc.fillRect(startX,startY,width,height);
            gc.setGlobalAlpha(1.0);

    }

    private void drawRain(int number) {
        int rain = number % 2 == 0 ? (int) Math.ceil(Math.random() * (MAX_RAINDROPS - MIN_RAINDROPS)) + MIN_RAINDROPS : 0;
        gc.setFill(Color.WHITE);
        gc.setGlobalAlpha(0.5);
        while (rain-- > 0) {
            double startX = prPlane.columnWidth * number;
            double startY = Math.random() * prPlane.height;
            double height = 50;

            gc.fillRect(startX, startY, 1, height);

        }
        gc.setGlobalAlpha(1.0);
        gc.setFill(Color.BLACK);
    }

    private double drawColumn(Ray ray, int number, double angle) {
        double distance = Double.POSITIVE_INFINITY;
        if (ray.rayPoints[Resources.Blocks.WALL] != null) {
            drawTexture(ray.rayPoints[Resources.Blocks.WALL], Resources.Textures.WALL, number, angle, Resources.Heights.WALL);
            distance = ray.rayPoints[Resources.Blocks.WALL].distance;
        }
        if (ray.rayPoints[Resources.Blocks.TARDIS] != null) {
            drawTexture(ray.rayPoints[Resources.Blocks.TARDIS], Resources.Textures.TARDIS, number, angle, Resources.Heights.TARDIS);
            distance = ray.rayPoints[Resources.Blocks.TARDIS].distance;
        }
         /*  if (distance>0.3)
                drawRain(number);*/
        return distance;
    }

    private void drawAngel(int number, double offset, double distance) {
        double startX = prPlane.columnWidth * number;
        double width = prPlane.columnWidth;

        Image texture = Resources.Textures.ANGEL;

        double texture_startX = texture.getWidth() * (offset + Angel.HALFWIDTH) / Angel.HALFWIDTH / 2;
        double texture_startY = 0;
        double texture_width = texture.getWidth() / resolution;
        double texture_height = texture.getHeight();

        double height = Resources.Heights.ANGEL * prPlane.height/*prPlane.distance*/ / distance;
        height += ((int) height) % 2;
        double startY = (prPlane.height / 2) * (1 + 1 / distance) - height;

        gc.drawImage(texture, texture_startX, texture_startY, texture_width, texture_height, startX, startY, width, height);

            gc.setGlobalAlpha(distance<SHADING_DISTANCE?distance/SHADING_DISTANCE:1.0);
            gc.drawImage(Resources.Textures.DARK_ANGEL,texture_startX,texture_startY,texture_width,texture_height,startX,startY,width,height);
            gc.setGlobalAlpha(1.0);
    }

    boolean buildColumn(Maze maze, Player player, int number, double alpha_angle, double distance_Ang_Pla) {
        boolean angelIsOnSight = false;
        double angle = Math.atan2(prPlane.columnWidth * number - prPlane.width / 2, prPlane.distance);

        Ray ray = new Ray((player.point_of_view - angle + CIRCLE) % CIRCLE, maze, player.coords);

        double distance = drawColumn(ray, number, angle);
        if ((distance_Ang_Pla < distance) && (Math.cos(alpha_angle - player.point_of_view) > cosAngelAngel)) {
            double angel_offset = distance_Ang_Pla * Math.sin(ray.angle - alpha_angle);
            if (Math.abs(angel_offset) < Angel.HALFWIDTH) {
                drawAngel(number, angel_offset, distance_Ang_Pla * Math.cos(ray.angle - alpha_angle));
                angelIsOnSight = true;
            }
        }
        if (distance>0.3)
            drawRain(number);
        return angelIsOnSight;
    }


    public void buildScreen(Maze maze, Player player, Angel angel) {
        gc.drawImage(Resources.Textures.SKY, 0, 0);

        double distance_Ang_Pla = Maze.distenceBetween(player.coords, angel.coords);
        double alpha_angle = Math.acos((angel.coords.x - player.coords.x) / distance_Ang_Pla) * (angel.coords.y - player.coords.y < 0 ? -1 : 1);
        alpha_angle = (alpha_angle + CIRCLE) % CIRCLE;

        angel.isOnSight = false;
        for (int i = 0; i < resolution; i++) {
            if (buildColumn(maze, player, i, alpha_angle, distance_Ang_Pla))
                angel.isOnSight = true;
        }
    }


    boolean falseScreen(Maze maze, Player player, Maze.Coords falseCoords) {
        double distance_Ang_Pla = Maze.distenceBetween(player.coords, falseCoords);
        double alpha_angle = Math.acos((falseCoords.x - player.coords.x) / distance_Ang_Pla) * (falseCoords.y - player.coords.y < 0 ? -1 : 1);

        boolean angelIsOnSight = false;
        for (int i = 0; i < resolution; i++) {
            if (falseBuildColumn(maze, player, i, alpha_angle, distance_Ang_Pla))
                angelIsOnSight = true;
        }
        return angelIsOnSight;
    }

    private boolean falseBuildColumn(Maze maze, Player player, int number, double alpha_angle, double distance_Ang_Pla) {
        boolean angelIsOnSight = false;
        double angle = Math.atan2(prPlane.columnWidth * number - prPlane.width / 2, prPlane.distance);

        Ray ray = new Ray((player.point_of_view - angle + CIRCLE) % CIRCLE, maze, player.coords);

        double distance = ray.rayPoints[Resources.Blocks.TARDIS]!=null?ray.rayPoints[Resources.Blocks.TARDIS].distance:
                          ray.rayPoints[Resources.Blocks.WALL]!=null?ray.rayPoints[Resources.Blocks.WALL].distance:
                          Double.POSITIVE_INFINITY;

        if ((distance_Ang_Pla < distance) && (Math.cos(alpha_angle - player.point_of_view) > cosAngelAngel)) {
            double angel_offset = distance_Ang_Pla * Math.sin(ray.angle - alpha_angle);
            if (Math.abs(angel_offset) < Angel.HALFWIDTH) {
                angelIsOnSight = true;
            }
        }
        return angelIsOnSight;
    }

    public void endGameScreen(String message){
        gc.setFill(Color.BLACK);
        gc.setGlobalAlpha(1.0);

        gc.fillRect(0,0,prPlane.width,prPlane.height);

        gc.setFill(Color.WHITE);
        gc.fillText(message,prPlane.width/2-message.length()/2,prPlane.height/3);

        gc.fillText("Restart?  [1]",prPlane.width/2-message.length()/2,prPlane.height/3+50);
        gc.fillText("Quit?     [2]",prPlane.width/2-message.length()/2,prPlane.height/3+75);
    }


}