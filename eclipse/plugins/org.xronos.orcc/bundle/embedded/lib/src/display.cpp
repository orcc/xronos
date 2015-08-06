/**
 * ----------------------------------------------------------------------------
 * __  ___ __ ___  _ __   ___  ___
 * \ \/ / '__/ _ \| '_ \ / _ \/ __|
 *  >  <| | | (_) | | | | (_) \__ \
 * /_/\_\_|  \___/|_| |_|\___/|___/
 * ----------------------------------------------------------------------------
 * Copyright (C) 2015 EPFL SCI STI MM
 *
 * This file is part of XRONOS.
 *
 * XRONOS is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * XRONOS is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with XRONOS.  If not, see <http://www.gnu.org/licenses/>.
 *
 * Additional permission under GNU GPL version 3 section 7
 *
 * If you modify this Program, or any covered work, by linking or combining it
 * with Eclipse (or a modified version of Eclipse or an Eclipse plugin or
 * an Eclipse library), containing parts covered by the terms of the
 * Eclipse Public License (EPL), the licensors of this Program grant you
 * additional permission to convey the resulting work.  Corresponding Source
 * for a non-source form of such a combination shall include the source code
 * for the parts of Eclipse libraries used as well as that of the  covered work.
 *
 */

#include <stdio.h>
#include <iostream>
#include <stdlib.h>

#ifndef NO_DISPLAY
#include <SDL/SDL.h>
static SDL_Surface *m_screen;
static SDL_Overlay *m_overlay;
#else
#include "timer.h"
static Timer timer;
#endif
#include "get_opt.h"

#ifndef NO_DISPLAY
static void press_a_key(int code) {
	char buf[2];
	char *ptrBuff = NULL;

	printf("Press a key to continue\n");
	ptrBuff=fgets(buf, 2, stdin);
	if(ptrBuff == NULL) {
		fprintf(stderr,"error when using fgets\n");
	}
	exit(code);
}
#endif

static unsigned int startTime;
static unsigned int relativeStartTime;
static int lastNumPic;
static int numPicturesDecoded;

void displayYUV_setSize(int width, int height)
{
#ifndef NO_DISPLAY
	//std::cout << "set display to " << width << " x " << height << std::endl;
	m_screen = SDL_SetVideoMode(width, height, 0, SDL_HWSURFACE);
	if (m_screen == NULL) {
		fprintf(stderr, "Couldn't set video mode!\n");
		press_a_key(-1);
	}

	if (m_overlay != NULL) {
		SDL_FreeYUVOverlay(m_overlay);
	}

	m_overlay = SDL_CreateYUVOverlay(width, height, SDL_YV12_OVERLAY, m_screen);
	if (m_overlay == NULL) {
		fprintf(stderr, "Couldn't create overlay: %s\n", SDL_GetError());
		press_a_key(-1);
	}
#endif
}

void displayYUV_displayPicture(unsigned char pictureBufferY[], unsigned char pictureBufferU[], unsigned char pictureBufferV[], short pictureWidth, short pictureHeight) 
{
#ifndef NO_DISPLAY
	static unsigned short lastWidth = 0;
	static unsigned short lastHeight = 0;

	SDL_Rect rect = { 0, 0, pictureWidth, pictureHeight };

	SDL_Event event;

	if((pictureHeight != lastHeight) || (pictureWidth != lastWidth)) {
		displayYUV_setSize(pictureWidth, pictureHeight);
		lastHeight = pictureHeight;
		lastWidth  = pictureWidth;
	}
	if (SDL_LockYUVOverlay(m_overlay) < 0) {
		fprintf(stderr, "Can't lock screen: %s\n", SDL_GetError());
		press_a_key(-1);
	}

	memcpy(m_overlay->pixels[0], pictureBufferY, pictureWidth * pictureHeight );
	memcpy(m_overlay->pixels[1], pictureBufferV, pictureWidth * pictureHeight / 4 );
	memcpy(m_overlay->pixels[2], pictureBufferU, pictureWidth * pictureHeight / 4 );

	SDL_UnlockYUVOverlay(m_overlay);
	SDL_DisplayYUVOverlay(m_overlay, &rect);

	/* Grab all the events off the queue. */
	while (SDL_PollEvent(&event)) {
		switch (event.type) {
		case SDL_KEYDOWN:
		case SDL_QUIT:
			exit(0);
			break;
		default:
			break;
		}
	}
#endif
}

void displayYUV_init()
{
#ifndef NO_DISPLAY
	// First, initialize SDL's video subsystem.
	if (SDL_Init( SDL_INIT_VIDEO ) < 0) {
		fprintf(stderr, "Video initialization failed: %s\n", SDL_GetError());
		press_a_key(-1);
	}

	SDL_WM_SetCaption("display", NULL);

	atexit(SDL_Quit);
#endif
}

/**
 * @brief Return the number of frames the user want to decode before exiting the application.
 * If user didn't use the -f flag, it returns -1 (DEFAULT_INFINITEà).
 * @return The
 */
int displayYUV_getNbFrames() 
{
	return nbFrames;
}

unsigned char displayYUV_getFlags()
{
	return 3;
}

void compareYUV_compareComponent(const int x_size, const int y_size, 
	const unsigned char *true_img_uchar, const unsigned char *test_img_uchar,
	unsigned char SizeMbSide, char Component_Type) 
{
}

void compareYUV_init()
{
}

void compareYUV_readComponent(unsigned char **Component, unsigned short width, unsigned short height, char sizeChanged)
{
}

void compareYUV_comparePicture(unsigned char pictureBufferY[], unsigned char pictureBufferU[],
	unsigned char pictureBufferV[], short pictureWidth,
	short pictureHeight)
{
}


static void print_fps_avg(void) {

#ifndef NO_DISPLAY
	unsigned int endTime = SDL_GetTicks();
#else
	unsigned int endTime = timer.getMilliseconds();
#endif
	printf("%i images in %f seconds: %f FPS\n", numPicturesDecoded,
		(float) (endTime - startTime)/ 1000.0f,
		1000.0f * (float) numPicturesDecoded / (float) (endTime -startTime));
}

void fpsPrintInit() {
#ifndef NO_DISPLAY
	startTime = SDL_GetTicks();
#else
	timer.reset();
	startTime = timer.getMilliseconds();
#endif
	relativeStartTime = startTime;
	numPicturesDecoded = 0;
	lastNumPic = 0;
	atexit(print_fps_avg);
}

void fpsPrintNewPicDecoded(void) {
	unsigned int endTime;
	numPicturesDecoded++;
#ifndef NO_DISPLAY
	endTime = SDL_GetTicks();
#else
	endTime = timer.getMilliseconds();
#endif
	if (endTime - relativeStartTime > 5000) {
		printf("%f images/sec\n", 1000.0f * (float) (numPicturesDecoded - lastNumPic) / (float) (endTime - relativeStartTime));
		relativeStartTime = endTime;
		lastNumPic = numPicturesDecoded;
	}
}
