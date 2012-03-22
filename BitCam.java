package bitcam;

import java.util.Random;
import java.util.logging.Logger;

import processing.core.PApplet;
import processing.core.PImage;

public class BitCam extends PApplet {

        PImage img;
        final int detailMin = 5;
        final int detailMax = 100;
        int detail = detailMin;
        int blur = 1;
        // number of clusters
        int N;
        // array of the centers
        // coordinates are in the 5d plane of x, y, r, g, b
        float[][] centers;
        // store which center a pixel is associated with
        short[] pixelCenters;

        // store how many times the pixel has changed centers
        short[] timesChanged;

        // store the Bitmap's data
        int[] orig;

        // store the number of pixels in each center
        int[] numPixels;

        // by what value do we multiply the color
        float CM;

        // what color pallete we are using.
        int[] colors;

        // the colors assigned to each center
        int[] centerColors;

        boolean first;

        int width, height;

        int K;

        int numChanged;

        long start;

        PImage test;

        boolean YUV = true;

        int pixelLength;

        int scale = 1;
        
        short[] timesUsed;
        


        public void setup() {

                test = loadImage("robertkelly.jpg");
                //size(test.width, test.height);
                //size(600, 600);
                
                vid = new GSCapture(this, 1280, 720);
                vid.play(); 
                colors = Pallete.commodore;
                init(test);
                start = System.currentTimeMillis();

                numChanged = width * height;

                image(compute(), 0, 0);
        }

        public void keyPressed() {
                if (key == 'c') {
                        colors = Pallete.commodore;
                        image(colorImage(), 0, 0);
                }
                if (key == 't') {
                        colors = Pallete.teletext;
                        image(colorImage(), 0, 0);
                }
                if (key == 's') {
                        colors = Pallete.sinclair;
                        image(colorImage(), 0, 0);
                }
                if (key == 'a') {
                        colors = Pallete.amstrad;
                        image(colorImage(), 0, 0);
                }
                if (key == 'i'){
                        colors = Pallete.ibmcga;
                        image(colorImage(), 0, 0);
                }

        }

        public PImage compute() {

                while (numChanged / (float) ((width * height)/scale) > .01) {
                        start = System.currentTimeMillis();
                        assignCenters();
                        computeCenters();

                        Logger.getAnonymousLogger().info(
                                        numChanged + " Pixels changed in "
                                                        + (System.currentTimeMillis() - start) / 1000F);

                }

                start = System.currentTimeMillis();
                computeCenterColors();
                return colorImage();
        }

        public void init(PImage img) {
                this.img = img;

                CM = 50F;

                // set the palette
                // colors = Pallete.commodore;
                // set the number of centers to the number of colors
                N = colors.length;

                // store the dimensions of the image so that we don't have to
                // call getWidth() and getHeight() all the time
                width = img.width;
                height = img.height;

                // initialize the data structures
                pixelCenters = new short[width * height];
                // orig = new int[width * height];
                centers = new float[N][5];

                // copy the data from the Bitmap into an array
                // img.getPixels(orig, 0, width, 0, 0, width,
                // height);
                img.loadPixels();
                orig = img.pixels;

                timesChanged = new short[width * height];

                pixelLength = pixelCenters.length;

                scaleFactor();
                // initialize the centers
                initCenters();
        }

        public void setPallete(int[] colors) {
                this.colors = colors;
        }

        public void setCenters(int centers) {
                N = centers;
        }

        private void initCenters() {
                Random random = new Random();
                int x, y, r, g, b;

                for (int k = 0; k < N; k++) {
                        x = random.nextInt(width);
                        y = random.nextInt(height);

                        r = random.nextInt(255);
                        g = random.nextInt(255);
                        b = random.nextInt(255);

                        centers[k] = new float[] { x, y, r, g, b };
                }
        }

        private void assignCenters() {
                // store the number of pixels in each center to compute the avg
                numPixels = new int[N];

                // reset the counter
                numChanged = 0;

                // the center closest to the pixel
                int minCenter;
                // the smallest distance between a pixel and a center
                float minValue;
                // the current value
                float curValue;

                float[] normalizedCenters;
                float[] normalizedPixel;

                // for every pixel in the image, assign it to a center
                int k;
                // for (int k = 0; k < pixelLength; k+=scale) {
                for (int x = 0; x < width; x += scale) {
                        for (int y = 0; y < height; y += scale) {

                                k = x + (y * width);

                                if (timesChanged[k] < 20) {
                                        minCenter = 0;
                                        minValue = 445;

                                        if (YUV) {
                                                normalizedPixel = normalizeYUV(k);
                                        } else {
                                                normalizedPixel = normalize(k);
                                        }

                                        for (int n = 0; n < N; n++) {
                                                if (YUV) {
                                                        normalizedCenters = normalizeYUV(centers[n]);

                                                        // compute the euclidean distance
                                                        curValue = (float) Math
                                                                        .sqrt(Math
                                                                                        .pow((normalizedCenters[0] - normalizedPixel[0]),
                                                                                                        2)
                                                                                        + Math.pow(
                                                                                                        (normalizedCenters[1] - normalizedPixel[1]),
                                                                                                        2)
                                                                                        + Math.pow(
                                                                                                        (normalizedCenters[2] - normalizedPixel[2]),
                                                                                                        2)
                                                                                        * 10
                                                                                        + Math.pow(
                                                                                                        (normalizedCenters[3] - normalizedPixel[3]),
                                                                                                        2)
                                                                                        * CM
                                                                                        + Math.pow(
                                                                                                        (normalizedCenters[4] - normalizedPixel[4]),
                                                                                                        2) * CM);
                                                } else {
                                                        normalizedCenters = normalize(centers[n]);

                                                        // compute the euclidean distance
                                                        curValue = (float) Math
                                                                        .sqrt(Math
                                                                                        .pow((normalizedCenters[0] - normalizedPixel[0]),
                                                                                                        2)
                                                                                        + Math.pow(
                                                                                                        (normalizedCenters[1] - normalizedPixel[1]),
                                                                                                        2)
                                                                                        + Math.pow(
                                                                                                        (normalizedCenters[2] - normalizedPixel[2]),
                                                                                                        2)
                                                                                        * 10
                                                                                        + Math.pow(
                                                                                                        (normalizedCenters[3] - normalizedPixel[3]),
                                                                                                        2)
                                                                                        * CM
                                                                                        + Math.pow(
                                                                                                        (normalizedCenters[4] - normalizedPixel[4]),
                                                                                                        2) * CM);

                                                }
                                                // if this center has the smallest distance, store it
                                                if (curValue < minValue) {
                                                        minValue = curValue;
                                                        minCenter = n;

                                                }

                                        }
                                } else {
                                        minCenter = pixelCenters[k];
                                }

                                if (pixelCenters[k] != minCenter) {
                                        // identify pixel with this center
                                        pixelCenters[k] = (short) minCenter;

                                        // note that it changed centers
                                        numChanged++;
                                        timesChanged[k]++;
                                }

                                // increment the number of pixel's this center has
                                numPixels[minCenter]++;

                                // p[k] = (((int) centers[minCenter][2] << 16) | ((int)
                                // centers[minCenter][3] << 8) | (int) centers[minCenter][4]);
                        }
                }

        }

        public PImage colorImage() {
                // create new Bitmap object
                // Bitmap ret = Bitmap.createBitmap(width, height,
                // Bitmap.Config.ARGB_8888);
                PImage ret = new PImage(width, height);

                // create an array that stores the color values of each pixel
                int[] retPixels = new int[width * height];
                int minCenter;

                computeCenterColors();

                int k, color;
                for (int x = 0; x < width; x += scale) {
                        for (int y = 0; y < height; y += scale) {
                                k = x + (y * width);
                                minCenter = pixelCenters[k];
                                color = centerColors[minCenter];
                                for (int x1 = 0; x1 < scale; x1++) {
                                        for (int y1 = 0; y1 < scale; y1++) {

                                                k = (x + x1) + ((y + y1) * width);
                                                if (k < pixelLength) {
                                                        retPixels[k] = color;
                                                }
                                        }
                                }

                        }
                }

                ret.pixels = retPixels;
                ret.updatePixels();

                return ret;
        }

        private void computeCenters() {
                // store the values for the new centers, do not make Pixel objects yet
                float[][] newValues = new float[N][5];

                int x, y, color, r, g, b;
                int curCenter;

                // for (int k = 0; k < pixelLength; k+=scale) {
                int k = 0;
                for (int X = 0; X < width; X += scale) {
                        for (int Y = 0; Y < height; Y += scale) {

                                k = X + (Y * width);
                                x = k % width;
                                y = k / width;

                                color = orig[k];

                                r = (color >> 16) & 0xFF;
                                g = (color >> 8) & 0xFF;
                                b = color & 0xFF;

                                // pixelCeters stores the center the pixel is currently aligned
                                // with
                                // take the pixels coordinates and add it to that center
                                curCenter = pixelCenters[k];

                                newValues[curCenter][0] += ((float) x / numPixels[curCenter]);
                                newValues[curCenter][1] += ((float) y / numPixels[curCenter]);
                                newValues[curCenter][2] += ((float) r / numPixels[curCenter]);
                                newValues[curCenter][3] += ((float) g / numPixels[curCenter]);
                                newValues[curCenter][4] += ((float) b / numPixels[curCenter]);

                        }
                }
                centers = newValues;
        }

        private void computeCenterColors() {
                timesUsed = new short[colors.length];
                int k;
                
                centerColors = new int[N];
                for (int i = 0; i < N; i++) {
                        for (int c = 0; c < colors.length; c++) {
                                k = getClosestColor((int) centers[i][2], (int) centers[i][3], (int) centers[i][4]);
                                timesUsed[k]++;
                                centerColors[i] = colors[k];
                        }
                }
        }

        private int getClosestColor(int r, int g, int b) {
                int colorI = 0;

                int r1, g1, b1, color;

                double distance;
                // max distance between points in RGB plane
                double oldDist = 445;

                for (int i = 0; i < colors.length; i++) {
                        color = colors[i];

                        r1 = (color >> 16) & 0xFF;
                        g1 = (color >> 8) & 0xFF;
                        b1 = color & 0xFF;

                        distance = Math.sqrt(Math.pow(r - r1, 2) + Math.pow(g - g1, 2)
                                        + Math.pow(b - b1, 2) + colors.length-1*timesUsed[i]);
                        if (distance < oldDist) {
                                colorI = i;
                                oldDist = distance;
                        }

                }
                return colorI;
        }

        private float[] normalize(float[] orig) {
                return new float[] { orig[0] / width, orig[1] / height, orig[2] / 255,
                                orig[3] / 255, orig[4] / 255 };
        }

        private float[] normalizeYUV(float[] orig) {
                int r = (int) orig[2];
                int g = (int) orig[3];
                int b = (int) orig[4];

                int y = (306 * r + 601 * g + 117 * b) >> 10;
                int u = ((-172 * r - 340 * g + 512 * b) >> 10) + 128;
                int v = ((512 * r - 429 * g - 83 * b) >> 10) + 128;

                return new float[] { orig[0] / width, orig[1] / height, y / 255F,
                                u / 255F, v / 255F };
        }

        private float[] normalize(int k) {
                // compute the pixels coordinates in the 5D plane. These were stored
                // in objects but the memory overhead was too high.

                float x = (k % width) / (float) width;
                float y = (k / width) / (float) height;

                float r = ((orig[k] >> 16) & 0xFF) / (float) 255;
                float g = ((orig[k] >> 8) & 0xFF) / (float) 255;
                float b = (orig[k] & 0xFF) / (float) 255;

                return new float[] { x, y, r, g, b };
        }

        private float[] normalizeYUV(int k) {
                float X = (k % width) / (float) width;
                float Y = (k / width) / (float) height;

                int r = ((orig[k] >> 16) & 0xFF);
                int g = ((orig[k] >> 8) & 0xFF);
                int b = (orig[k] & 0xFF);

                int y = (306 * r + 601 * g + 117 * b) >> 10;
                int u = ((-172 * r - 340 * g + 512 * b) >> 10) + 128;
                int v = ((512 * r - 429 * g - 83 * b) >> 10) + 128;

                return new float[] { X, Y, y / 255F, u / 255F, v / 255F };
        }

        public int convertRGBtoYUV(int color) {

                int r = (color >> 16) & 0xFF;
                int g = (color >> 8) & 0xFF;
                int b = color & 0xFF;

                int y = (306 * r + 601 * g + 117 * b) >> 10;
                int u = ((-172 * r - 340 * g + 512 * b) >> 10) + 128;
                int v = ((512 * r - 429 * g - 83 * b) >> 10) + 128;
                constrain(y, 0, 255);
                constrain(u, 0, 255);
                constrain(v, 0, 255);

                // return color(y,u,v);
                return 0;
        }

        public void pixelate(PImage img) {
                for (int i = 0; i < img.width; i += detail) {
                        for (int j = 0; j < img.height; j += detail) {
                                int c = test.get(i, j);
                                fill(c);
                                stroke(c);
                                rect(i, j, detail, detail);
                        }
                }
                if (detail <= detailMin) {
                        detail = detailMin;
                        blur = 1;
                } else if (detail > detailMax) {
                        detail = detailMax;
                        blur = -1;
                }
                detail += blur * 3;
        }

        void drawCenters() {
                float x, y, r, g, b;
                for (int k = 0; k < N; k++) {
                        fill(centers[k][2], centers[k][3], centers[k][4]);
                        // noStroke();
                        ellipse(centers[k][0], centers[k][1], 10, 10);
                }
        }

        // used to listen to keyboard input
        public void draw() {
                /*
                background(0);
                vid.read();
                vid.loadPixels();
                orig = vid.pixels;
                
                assignCenters();
                computeCenters();
                computeCenterColors();
                image(colorImage(), 0, 0);
                */
                
        }

        public void scaleFactor() {
                if (width > 64 || height > 64) {
                        // http://stackoverflow.com/questions/477572/android-strange-out-of-memory-issue/823966#823966
                        scale = (int) Math.pow(
                                        2.0,
                                        (int) Math.round(Math.log(256 / (double) Math.mawidth,
                                                        height)) / Math.log(0.5)));
                }
        }

        public static void main(String[] args) {
                PApplet.main(new String[] { bitcam.BitCam.class.getName() });
        }
}
