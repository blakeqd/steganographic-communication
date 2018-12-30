package com.example.stego.communication;

import java.util.ArrayList;

public class ImageStuff {
	
	private static int SECTION_LENGTH; //= 1000;
	private static int NUMBER_OF_SECTIONS; // = 5;
	
	
	private static int numberOfSectionsMsg;
	
	private static ArrayList<Double> sectionsRating;
	private static int[] sectionsBest;
	private static double[] messageSectionsRating;
	
	private static double probOfMsgBit0 = 0.49;
	private double probOfMsgBit1 = 0.51;
	
	// Rate sections based on the number of 1's in the LSBS
	// p = pixels of the img
	public static void rateSections(int[] p, int sec_length, int num_of_sections)
	{
		SECTION_LENGTH = sec_length;
		NUMBER_OF_SECTIONS = num_of_sections;
		sectionsRating = new ArrayList<Double>(NUMBER_OF_SECTIONS);
		
		
		int currentPixel = 0;
		int currentLSB = 0;
		
		int numOfOnes;
		for (int i = 0; i < NUMBER_OF_SECTIONS; i++)
		{
			numOfOnes = 0;
			
			for (int j = currentPixel; j < (currentPixel + SECTION_LENGTH); j++)
			{
				// GET THE current lsb of the img
				if (j >= p.length)
					break;
				
				
				currentLSB = ( (p[j] >> 16) & 0xFF ) % 2;
				if (currentLSB == 1) numOfOnes++;
				
				currentLSB = ( (p[j] >> 8) & 0xFF ) % 2; 
				if (currentLSB == 1) numOfOnes++;
				
				currentLSB = ( p[j] & 0xFF ) % 2; 
				if (currentLSB == 1) numOfOnes++;
				
			}
			double rating = ((double) numOfOnes) / (3*SECTION_LENGTH);
			double roundedRating = (double)Math.round(rating * 100000) / 100000;
			
			sectionsRating.add(i, roundedRating);
			currentPixel += SECTION_LENGTH;
		}
	}
	
	
	// Get the best sections by choosing sections with the number of 1's equal to the number
	// of 0's in the message.
	public static int[] getBestSections(int numOfSec, int[] m)
	{
		numberOfSectionsMsg = numOfSec;
		sectionsBest = new int[numberOfSectionsMsg];
		messageSectionsRating = new double[numberOfSectionsMsg];
		
		setMessageSectionRatings(m, messageSectionsRating);
		
		for (int i = 0; i < messageSectionsRating.length; i++)
		{	
			sectionsBest[i] = -1;
			double range = 0.00000;
			double a = messageSectionsRating[i], b = messageSectionsRating[i];
			
			while (sectionsBest[i] == -1)
			{	
				if (sectionsRating.contains( a) )
				{
					sectionsBest[i] = sectionsRating.indexOf( a );
					sectionsRating.set(sectionsBest[i], -1.0); // Mark that the section has been chosen already
					
				}
				else if (sectionsRating.contains( b ) )
				{
					sectionsBest[i] = sectionsRating.indexOf( b );
					sectionsRating.set(sectionsBest[i], -1.0); // Mark that the section has been chosen already
				}
				range += 0.00001;
				range = (double)Math.round((range) * 100000) / 100000;
				
				a = (double)Math.round((messageSectionsRating[i] + range) * 100000) / 100000;
				b = (double)Math.round((messageSectionsRating[i] - range) * 100000) / 100000;
			}
		}
		
		return sectionsBest; 
	}
	
	
	
	public static void setMessageSectionRatings(int[] m, double[] mRating)
	{
		int start;
		
		int numOfZeros = 0;
		double zerosRating,  roundedRating;
		
		for (int i = 0; i < mRating.length; i++)
		{
			numOfZeros = 0;
			
			start = i*(3*SECTION_LENGTH);
			if (start+(3*SECTION_LENGTH) > m.length)
				break;
			
			for (int j = start; j < (start + (3*SECTION_LENGTH)); j++)
			{
				if (m[j] == 0) numOfZeros++;
			
			}
			zerosRating = ((double) numOfZeros) / (3*SECTION_LENGTH) ;
			roundedRating = (double)Math.round(zerosRating * 100000) / 100000;
			mRating[i] = roundedRating;
		}
	}
}
