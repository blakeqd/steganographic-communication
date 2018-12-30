package com.example.stego.communication;


import java.io.BufferedReader;    
import java.io.File;  
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;

import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.CursorLoader;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.AssetManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

/**
 * The three steganographic methods are implemented in onModifyButtonSequential(), onModifyButtonRandom(), and
 * onModifyButtonLetterFrequency().
 * 
 * @author Blake Quebec Desloges
 *
 */
public class MainActivity extends Activity implements OnClickListener{

	private static final int CAPTURE_IMAGE_ACTIVITY_REQUEST_CODE = 100;
	private static final int VIEW_THUMBNAILS = 200;
	
	public static final int MEDIA_TYPE_IMAGE = 1;
	public static final int MEDIA_TYPE_VIDEO = 2;
	
	
	private static int SECTION_LENGTH = 1000;
	private static int NUMBER_OF_SECTIONS;
	
	private int numOfPixels;
	private int numOfLSBs;
	private int numOfCharacters;
	
	
	private Intent intent;
	private Intent thumbnailIntent;
	private File imageFile;
	private Bitmap img;
	
	private Bitmap imgCopy;
	
	
	private Button capture;
	private Button select;
	private Button modifySeq, modifyRand, modifyLF;
	private Button decode;
	private Button save;
	private Button enterMsg;
	private TextView textTargetUri;
	
	private TextView pixels, characters, lsbs;

	
	private AssetManager am;
	private InputStream is;
	private BufferedReader reader;
	private String line;
	
	private String message = "";
	
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
    	requestWindowFeature(Window.FEATURE_NO_TITLE);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        // BUTTONS
        capture = (Button) findViewById(R.id.capture_button);
        capture.setOnClickListener(this);
        select = (Button) findViewById(R.id.select_button);
        select.setOnClickListener(this);
        modifySeq = (Button) findViewById(R.id.modify_button_sequential);
        modifySeq.setOnClickListener(this);
        modifyRand = (Button) findViewById(R.id.modify_button_random);
        modifyRand.setOnClickListener(this);
        modifyLF = (Button) findViewById(R.id.modify_button_letter_frequency);
        modifyLF.setOnClickListener(this);
        decode = (Button) findViewById(R.id.decode_button);
        decode.setOnClickListener(this);
        save = (Button) findViewById(R.id.save_button);
        save.setOnClickListener(this);
        enterMsg = (Button) findViewById(R.id.enter_msg);
        enterMsg.setOnClickListener(this);
        
        
        // TEXT
        textTargetUri = (TextView)findViewById(R.id.targeturi);
        textTargetUri.setText("No image selected.");
        
        pixels = (TextView)findViewById(R.id.pixels);
        pixels.append("0");
        characters = (TextView)findViewById(R.id.characters);
        characters.append("0");
        lsbs = (TextView)findViewById(R.id.lsbs);
        lsbs.append("0");
        
        img = null;
        
        
        am = this.getAssets();

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.activity_main, menu);
        return true;
    }

	@Override
	public void onClick(View v) {
		if (v.getId() == R.id.capture_button)
		{
			onCaptureButton();
		}
		if (v.getId() == R.id.select_button)
		{
			onSelectButton();
		}
		if (v.getId() == R.id.modify_button_sequential)
		{
			onModifyButtonSequential();
		}
		if (v.getId() == R.id.modify_button_letter_frequency)
		{
			onModifyButtonLetterFrequency();
		}
		if (v.getId() == R.id.modify_button_random)
		{
			onModifyButtonRandom();
		}
		if (v.getId() == R.id.decode_button)
		{
			onDecodeButton();
		}
		if (v.getId() == R.id.save_button)
		{
			onSaveButton();
		}
		if (v.getId() == R.id.enter_msg)
		{
			onEnterMsg();
		}
	}


	

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
	    if (requestCode == CAPTURE_IMAGE_ACTIVITY_REQUEST_CODE) 
	    {
	        if (resultCode == RESULT_OK) {
	            // Image captured and saved to fileUri specified in the Intent
	        	
	            Toast.makeText(this, "Image saved to:\n" +
	                     data.getData(), Toast.LENGTH_LONG).show();
	        } else if (resultCode == RESULT_CANCELED) {
	            // User cancelled the image capture
	        	Toast.makeText(this, "CANCELLED", Toast.LENGTH_LONG).show();
	        } else {
	            // Image capture failed, advise user
	        	Toast.makeText(this, "ERROR", Toast.LENGTH_LONG).show();
	        }
	    }
	    if (requestCode == VIEW_THUMBNAILS)
	    {
	    	if (resultCode == RESULT_OK){
	    		Uri targetUri = data.getData();
	    		String realPath = getRealPathFromURI(targetUri);
	    		textTargetUri.setText("IMAGE SELECTED: " + realPath);
	    		
	    		
	    		imageFile = new File(realPath);
	    		img = BitmapFactory.decodeFile(realPath);
	    		imgCopy = img.copy(Bitmap.Config.ARGB_8888, true);

	    		numOfPixels = img.getHeight() * img.getWidth();
	    		numOfLSBs = numOfPixels * 3;
	    		numOfCharacters = numOfLSBs / 7;
	    		
	    		pixels.setText("Number of Pixels: " + numOfPixels);
	    		characters.setText("Max Length of Message: " + numOfCharacters);
	    		lsbs.setText("Number of LSB's: " + numOfLSBs);
	    		
	    		
	    		NUMBER_OF_SECTIONS = ( (numOfPixels / 1000) > 3000 ) ? 3000 : numOfPixels / 1000;
	    		System.out.println("MSG # of sections: " + NUMBER_OF_SECTIONS);

	    	}
	    }
	    if (requestCode == 0)
	    {
	    	if (resultCode == RESULT_OK){
	    			
	    	}
	    }
	}
	
	
	public void onCaptureButton(){
		// create Intent to take a picture and return control to the calling application
        intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        
		// start the image capture Intent
	    startActivityForResult(intent, CAPTURE_IMAGE_ACTIVITY_REQUEST_CODE);
	    
	}

	private void onSelectButton() {
		// Select image from storage
		Intent thumbnailIntent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
		// Start image selection intent
		startActivityForResult(thumbnailIntent, VIEW_THUMBNAILS);
	}
	
	
	// Implement sequential LSB embedding
	private void onModifyButtonSequential() {	
		
		// Prepare for image to be modified ----------------------------------
		int height = 0, width = 0;
		if (imgCopy != null){
			height = imgCopy.getHeight();
			width = imgCopy.getWidth();
		}
		
		int[] pixels = new int[height*width];
		
		// Get the pixels of the image
		imgCopy.getPixels(pixels, 0, width, 0, 0, width, height);
		
			
		// Begin modification process ---------------------------------------
		byte[] messageBytes = null;
		
		//Convert message to ASCII
		try {
			messageBytes = message.getBytes("US-ASCII");
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
		
		// Convert ASCII message to bit stream
		// messageBitStream will hold the binary representation of a character
		int[] messageBitStream = new int[messageBytes.length*7]; 
		

		int j, currentChar;
		for (int i = 1; i <= messageBytes.length; i++)
		{
			currentChar = messageBytes[i-1];
			j = i*7;
			
			messageBitStream[j-1] =  currentChar % 2; currentChar =  (currentChar >> 1);
			messageBitStream[j-2] =  currentChar % 2; currentChar =  (currentChar >> 1);
			messageBitStream[j-3] =  currentChar % 2; currentChar =  (currentChar >> 1);
			messageBitStream[j-4] =  currentChar % 2; currentChar =  (currentChar >> 1);
			messageBitStream[j-5] =  currentChar % 2; currentChar =  (currentChar >> 1);
			messageBitStream[j-6] =  currentChar % 2; currentChar =  (currentChar >> 1);
			messageBitStream[j-7] =  currentChar % 2; 
		}
		
		int lengthOfMessageInBits = messageBitStream.length;
		
		// Since each pixel is an 32-bit integer split up into alpha, red, green, and blue components,
		// I only encode the message in the LSB's of the red, green, and blue components.
		// This means in each pixel I can encode 3 bits.
		int curMsgBit = 0;
		
		int r,g,b;
	    for (int i = 0; i < pixels.length; i++)
	    {

	    	int c = pixels[i];
				
			r = (c >> 16) & 0xFF; g = (c >> 8) & 0xFF; b = c & 0xFF; 
				
				
			int lsb = r % 2; 
				
			if ( lsb == 0 && messageBitStream[curMsgBit] == 1)
				r++;
			else if ( lsb == 1 && messageBitStream[curMsgBit] == 0 )
				r--;
				
			curMsgBit++; // Move to the next bit in the message
	    	if ( curMsgBit >= lengthOfMessageInBits )
			{
				pixels[i] = Color.rgb(r, g, b);
				break;
			}
	    		
	    		
	    	lsb = g % 2;
			if ( lsb == 0 && messageBitStream[curMsgBit] == 1)
				g++;
			else if ( lsb == 1 && messageBitStream[curMsgBit] == 0 )
				g--;
				
			curMsgBit++; // Move to the next bit in the message
			if ( curMsgBit >= lengthOfMessageInBits )
			{
				pixels[i] = Color.rgb(r, g, b);
				break;
			}
				
			lsb = b % 2; //ImageStuff.getLSB(c,curLSB);
			if ( lsb == 0 && messageBitStream[curMsgBit] == 1)
				b++;
			else if ( lsb == 1 && messageBitStream[curMsgBit] == 0 )
				b--;
				
			curMsgBit++; // Move to the next bit in the message
			if ( curMsgBit >= lengthOfMessageInBits )
			{
				pixels[i] = Color.rgb(r, g, b);
				break;
			}
				
			pixels[i] = Color.rgb(r, g, b);
	    	
	    }

		// Save modified image which contains the secret message in the LSB's
		imgCopy.setPixels(pixels, 0, width, 0, 0, width, height);
		Toast.makeText(this, "Message Encoded Successfully.", Toast.LENGTH_LONG).show();
	}
	
	

	
	// Implement the new steganographic embedding method discussed in my thesis.
	private void onModifyButtonLetterFrequency() {
		System.out.println("MSG msg length: " + message.length() );
		
		
		// Prepare for image to be modified ----------------------------------
		int height = 0, width = 0;
		if (imgCopy != null){
			height = imgCopy.getHeight();
			width = imgCopy.getWidth();
		}
		
		int[] pixels = new int[height*width];
		
		// Get the pixels of the image
		imgCopy.getPixels(pixels, 0, width, 0, 0, width, height);
		
		// Number of sections to split up the image into
		NUMBER_OF_SECTIONS = numOfPixels / SECTION_LENGTH;
		// Assign a rating to each image section
		ImageStuff.rateSections(pixels, SECTION_LENGTH, NUMBER_OF_SECTIONS);

		
		// Begin modification process ---------------------------------------
		byte[] messageBytes = null;
		
		//Convert message to ASCII
		try {
			messageBytes = message.getBytes("US-ASCII");
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
		
		// Convert ASCII message to bit stream
		// 	messageBitStream will hold the binary representation of a character
		int[] messageBitStream = new int[messageBytes.length*7]; 
		
		byte currentChar = 0;
		for (int i = 1; i <= messageBytes.length; i++)
		{
			currentChar = messageBytes[i-1];

			int j = i*7;
			
			messageBitStream[j-1] =  currentChar % 2; currentChar = (byte) (currentChar >> 1);
			messageBitStream[j-2] =  currentChar % 2; currentChar = (byte) (currentChar >> 1);
			messageBitStream[j-3] =  currentChar % 2; currentChar = (byte) (currentChar >> 1);
			messageBitStream[j-4] =  currentChar % 2; currentChar = (byte) (currentChar >> 1);
			messageBitStream[j-5] =  currentChar % 2; currentChar = (byte) (currentChar >> 1);
			messageBitStream[j-6] =  currentChar % 2; currentChar = (byte) (currentChar >> 1);
			messageBitStream[j-7] =  currentChar % 2; 

		}
		
		
		int numOfSectionsForMessage = (int) Math.ceil(  (message.length() * 2.334) / SECTION_LENGTH ); // 2.334 pixels / char
		int[] sections = ImageStuff.getBestSections(numOfSectionsForMessage, messageBitStream);
		int lengthOfMessageInBits = messageBitStream.length;
		
		// Since each pixel is an 32-bit integer split up into alpha, red, green, and blue components,
		// I only encode the message in the LSB's of the red, green, and blue components.
		// This means in each pixel I can encode 3 bits.
		int curMsgBit = 0;
	    int curPixel = 0;
		int r,g,b;
	    for (int i = 0; i < sections.length; i++)
	    {
	    	curPixel = sections[i] * SECTION_LENGTH;
			System.out.println("MSG    Embedding section " + sections[i]+ " of " + sections.length);
			
			int endOfSection = curPixel + SECTION_LENGTH;
	    	for (int j = curPixel; j < endOfSection; j++)
	    	{
	    		int c = pixels[j];
				
				r = (c >> 16) & 0xFF; g = (c >> 8) & 0xFF; b = c & 0xFF;
				
				int lsb = r % 2 ;
				
				if ( lsb == 0 && messageBitStream[curMsgBit] == 1)
					r++;
				else if ( lsb == 1 && messageBitStream[curMsgBit] == 0 )
					r--;
				
				curMsgBit++; // Move to the next bit in the message
	    		if ( curMsgBit >= lengthOfMessageInBits )
				{
					pixels[j] = Color.rgb(r, g, b);
					break;
				}
	    		
	    		
	    		lsb = g % 2; 
				if ( lsb == 0 && messageBitStream[curMsgBit] == 1)
					g++;
				else if ( lsb == 1 && messageBitStream[curMsgBit] == 0 )
					g--;
				
				curMsgBit++; // Move to the next bit in the message
				if ( curMsgBit >= lengthOfMessageInBits )
				{
					pixels[j] = Color.rgb(r, g, b);
					break;
				}
				
				lsb = b % 2;
				if ( lsb == 0 && messageBitStream[curMsgBit] == 1)
					b++;
				else if ( lsb == 1 && messageBitStream[curMsgBit] == 0 )
					b--;
				
				curMsgBit++; // Move to the next bit in the message
				if ( curMsgBit >= lengthOfMessageInBits )
				{
					pixels[j] = Color.rgb(r, g, b);
					break;
				}
				
				pixels[j] = Color.rgb(r, g, b);
	    	}
	    	
	    	if (curMsgBit >= lengthOfMessageInBits)
	    		break;
	    	
	    }	
		
		// Save modified image which contains the secret message in the LSB's
		imgCopy.setPixels(pixels, 0, width, 0, 0, width, height);
		Toast.makeText(this, "Message Encoded Successfully.", Toast.LENGTH_LONG).show();
		
	
	}
	
	// Implement random section embedding
	private void onModifyButtonRandom() {

		System.out.println("MSG msg length: " + message.length() );
		
		
		// Prepare for image to be modified ----------------------------------
		int height = 0, width = 0;
		if (imgCopy != null){
			height = imgCopy.getHeight();
			width = imgCopy.getWidth();
		}
		
		int[] pixels = new int[height*width];
		
		// Get the pixels of the image
		imgCopy.getPixels(pixels, 0, width, 0, 0, width, height);
		
		// Number of sections to split up the image into
		NUMBER_OF_SECTIONS = numOfPixels / SECTION_LENGTH;
		
		int numOfSectionsForMessage = (int) Math.ceil(  (message.length() * 2.5) / SECTION_LENGTH );
		int[] sections = new int[numOfSectionsForMessage];
		
		for (int i = 0; i < sections.length; i++)
		{
			int r = (int) Math.round( Math.random() * (NUMBER_OF_SECTIONS-1) );
			
			int j = 0;
			while ( j < sections.length)
			{
				if ( r == sections[j] ){
					r = (int) Math.round( Math.random() * (NUMBER_OF_SECTIONS-1) );
					j = 0;
				}
				else
					j++;
			}
			
			System.out.println("MSG random section is " + r );
			sections[i] = r;
		}


		
		// Begin modification process ---------------------------------------
		byte[] messageBytes = null;
		
		//Convert message to ASCII
		try {
			messageBytes = message.getBytes("US-ASCII");
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
		
		// Convert ASCII message to bit stream
		// 	messageBitStream will hold the binary representation of a character
		int[] messageBitStream = new int[messageBytes.length*7]; 
		
		byte currentChar = 0;
		
		for (int i = 1; i <= messageBytes.length; i++)
		{
			currentChar = messageBytes[i-1];

			int j = i*7;
			
			messageBitStream[j-1] =  currentChar % 2; currentChar = (byte) (currentChar >> 1);
			messageBitStream[j-2] =  currentChar % 2; currentChar = (byte) (currentChar >> 1);
			messageBitStream[j-3] =  currentChar % 2; currentChar = (byte) (currentChar >> 1);
			messageBitStream[j-4] =  currentChar % 2; currentChar = (byte) (currentChar >> 1);
			messageBitStream[j-5] =  currentChar % 2; currentChar = (byte) (currentChar >> 1);
			messageBitStream[j-6] =  currentChar % 2; currentChar = (byte) (currentChar >> 1);
			messageBitStream[j-7] =  currentChar % 2; 

		}
		
		int lengthOfMessageInBits = messageBitStream.length;
		
		// Since each pixel is an 32-bit integer split up into alpha, red, green, and blue components,
		// I only encode the message in the LSB's of the red, green, and blue components.
		// This means in each pixel I can encode 3 bits.
		int curMsgBit = 0;
	    int curPixel = 0;
		int r,g,b;
	    for (int i = 0; i < sections.length; i++)
	    {
	    	curPixel = sections[i] * SECTION_LENGTH;
			
			int endOfSection = curPixel + SECTION_LENGTH;
	    	for (int j = curPixel; j < endOfSection; j++)
	    	{
	    		int c = pixels[j];

				
				r = (c >> 16) & 0xFF; g = (c >> 8) & 0xFF; b = c & 0xFF;
				
				int lsb = r % 2 ; 
				
				if ( lsb == 0 && messageBitStream[curMsgBit] == 1)
					r++;
				else if ( lsb == 1 && messageBitStream[curMsgBit] == 0 )
					r--;
				
				curMsgBit++; // Move to the next bit in the message
	    		if ( curMsgBit >= lengthOfMessageInBits )
				{
					pixels[j] = Color.rgb(r, g, b);
					break;
				}
	    		
	    		
	    		lsb = g % 2; 
				if ( lsb == 0 && messageBitStream[curMsgBit] == 1)
					g++;
				else if ( lsb == 1 && messageBitStream[curMsgBit] == 0 )
					g--;
				
				curMsgBit++; // Move to the next bit in the message
				if ( curMsgBit >= lengthOfMessageInBits )
				{
					pixels[j] = Color.rgb(r, g, b);
					break;
				}
				
				lsb = b % 2; 
				if ( lsb == 0 && messageBitStream[curMsgBit] == 1)
					b++;
				else if ( lsb == 1 && messageBitStream[curMsgBit] == 0 )
					b--;
				
				curMsgBit++; // Move to the next bit in the message
				if ( curMsgBit >= lengthOfMessageInBits )
				{
					pixels[j] = Color.rgb(r, g, b);
					break;
				}
				
				pixels[j] = Color.rgb(r, g, b);
	    	}
	    	
	    	if (curMsgBit >= lengthOfMessageInBits)
	    		break;
	    	
	    }

		// Save modified image which contains the secret message in the LSB's
		imgCopy.setPixels(pixels, 0, width, 0, 0, width, height);
		Toast.makeText(this, "Message Encoded Successfully.", Toast.LENGTH_LONG).show();
	}
	
	
	
	
	// Method to decode the first 30 characters embedded sequentially in an image
	private void onDecodeButton() {
		if (imgCopy == null)
			return;
		
		String decodedMessage = "";
		int[][] messageBitStream = new int[30][7]; // Predefined value
		
			
		
		int height = imgCopy.getHeight();
		int width = imgCopy.getWidth();
		
		int[] pixels = new int[height*width];
		
		// Get the pixels of the image
		imgCopy.getPixels(pixels, 0, width, 0, 0, width, height);
		


		
		// Obtain the message bits
		int lengthOfMessageInBits = messageBitStream.length * messageBitStream[0].length;
		int curBit = 0;
		for (int i = 0; i < pixels.length; i++)
		{
				int c = pixels[i];
				int r = Color.red(c), g = Color.green(c), b = Color.blue(c);
				
				if ( r % 2 == 0 )
					messageBitStream[ curBit / 7 ][ curBit % 7] = 0;
				else
					messageBitStream[ curBit / 7 ][ curBit % 7] = 1;
				
				
				curBit++;
				if ( curBit >= lengthOfMessageInBits )
				{
					break;
				}
				
				
				
				if ( g % 2 == 0 )
					messageBitStream[ curBit / 7 ][ curBit % 7] = 0;
				else
					messageBitStream[ curBit / 7 ][ curBit % 7] = 1;
				
				curBit++;
				if ( curBit >= lengthOfMessageInBits )
				{
					break;
				}
				
				
				
				if ( b % 2 == 0 )
					messageBitStream[ curBit / 7 ][ curBit % 7] = 0;
				else
					messageBitStream[ curBit / 7 ][ curBit % 7] = 1;
				
				curBit++;
				if ( curBit >= lengthOfMessageInBits )
				{
					break;
				}
			
		}
		

		
		String[] characters = new String[messageBitStream.length];
		int charCode;
		String str;
		
		for (int i = 0; i < characters.length; i++)
		{
			characters[i] = "";
			for (int j = 0; j < 7; j++)
			{
				String tmp = characters[i];
				characters[i] = tmp + messageBitStream[i][j];
			}
			
			charCode = Integer.parseInt(characters[i], 2);
			
			str = Character.valueOf((char)charCode).toString();
			decodedMessage += str;
			
		}
		
		Toast.makeText(this, "Hidden message: " + decodedMessage, Toast.LENGTH_LONG).show();
		System.out.println("MSG DECODED: " + decodedMessage);
	}
	
	
	
	// Save the modified image
	private void onSaveButton() {
		SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMdd_HHmmss");
		Date date = new Date();
		String d = dateFormat.format(date);
		
		File modImg = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), "stegtest_" + d + ".PNG");

		try{
			FileOutputStream out = new FileOutputStream(modImg);
			imgCopy.compress(Bitmap.CompressFormat.PNG, 100, out);

			out.flush();
            out.close();
            sendBroadcast(new Intent(Intent.ACTION_MEDIA_MOUNTED, Uri.parse("file://"+ Environment.getExternalStorageDirectory())));
            
			Toast.makeText(this, "Image saved as: " + modImg.getAbsolutePath(), Toast.LENGTH_LONG).show();
			
		}
		catch (Exception e) {
		       System.out.println("MSG " + e.toString());
		}
		
	}
	
	
	
	// Obtain a message from user input
	private void onEnterMsg() {
		AlertDialog.Builder alert = new AlertDialog.Builder(this);

		alert.setTitle("Enter a secret message!");
		alert.setMessage("Message");

		// Set an EditText view to get user input 
		final EditText input = new EditText(this);
		alert.setView(input);

		alert.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
		public void onClick(DialogInterface dialog, int whichButton) {
		  String value = input.getText().toString();
		  message = "";  // Reset message to empty string
		  
		  // These files contained different amounts of text and were used in the experiement for my thesis.
		  if (value.equals("hamlet"))
		  {
			  try {
					is = am.open("Hamlet_tiny.txt");
					reader = new BufferedReader(new InputStreamReader(is));
					
					while ( (line = reader.readLine()) != null )
					{
						message += line; //  + "\n";
					}
					
				} catch (IOException e) {
					e.printStackTrace();
				}
		  }
		  else if (value.equals("hamlet2"))
		  {
			  try {
					is = am.open("Hamlet_small.txt");
					reader = new BufferedReader(new InputStreamReader(is));
					
					while ( (line = reader.readLine()) != null )
					{
						message += line; //  + "\n";
					}
					
				} catch (IOException e) {
					e.printStackTrace();
				}
		  }
		  else if (value.equals("bridge25"))
		  {
			  try {
					is = am.open("hamlet_bridge_25.txt");
					reader = new BufferedReader(new InputStreamReader(is));
					
					while ( (line = reader.readLine()) != null )
					{
						message += line; //  + "\n";
					}
					
				} catch (IOException e) {
					e.printStackTrace();
				}
		  }
		  else if (value.equals("bridge50"))
		  {
			  try {
					is = am.open("hamlet_bridge_50.txt");
					reader = new BufferedReader(new InputStreamReader(is));
					
					while ( (line = reader.readLine()) != null )
					{
						message += line; //  + "\n";
					}
					
				} catch (IOException e) {
					e.printStackTrace();
				}
		  }
		  else if (value.equals("bridge75"))
		  {
			  try {
					is = am.open("hamlet_bridge_75.txt");
					reader = new BufferedReader(new InputStreamReader(is));
					
					while ( (line = reader.readLine()) != null )
					{
						message += line; //  + "\n";
					}
					
				} catch (IOException e) {
					e.printStackTrace();
				}
		  }
		  else if (value.equals("tree25"))
		  {
			  try {
					is = am.open("hamlet_tree_25.txt");
					reader = new BufferedReader(new InputStreamReader(is));
					
					while ( (line = reader.readLine()) != null )
					{
						message += line; //  + "\n";
					}
					
				} catch (IOException e) {
					e.printStackTrace();
				}
		  }
		  else if (value.equals("tree50"))
		  {
			  try {
					is = am.open("hamlet_tree_50.txt");
					reader = new BufferedReader(new InputStreamReader(is));
					
					while ( (line = reader.readLine()) != null )
					{
						message += line; //  + "\n";
					}
					
				} catch (IOException e) {
					e.printStackTrace();
				}
		  }
		  else if (value.equals("tree75"))
		  {
			  try {
					is = am.open("hamlet_tree_75.txt");
					reader = new BufferedReader(new InputStreamReader(is));
					
					while ( (line = reader.readLine()) != null )
					{
						message += line; //  + "\n";
					}
					
				} catch (IOException e) {
					e.printStackTrace();
				}
		  }
		  else if (value.equals("landscape25"))
		  {
			  try {
					is = am.open("hamlet_landscape_25.txt");
					reader = new BufferedReader(new InputStreamReader(is));
					
					while ( (line = reader.readLine()) != null )
					{
						message += line; //  + "\n";
					}
					
				} catch (IOException e) {
					e.printStackTrace();
				}
		  }
		  else if (value.equals("landscape50"))
		  {
			  try {
					is = am.open("hamlet_landscape_50.txt");
					reader = new BufferedReader(new InputStreamReader(is));
					
					while ( (line = reader.readLine()) != null )
					{
						message += line; //  + "\n";
					}
					
				} catch (IOException e) {
					e.printStackTrace();
				}
		  }
		  else if (value.equals("landscape75"))
		  {
			  try {
					is = am.open("hamlet_landscape_75.txt");
					reader = new BufferedReader(new InputStreamReader(is));
					
					while ( (line = reader.readLine()) != null )
					{
						message += line; //  + "\n";
					}
					
				} catch (IOException e) {
					e.printStackTrace();
				}
		  }
		  else if (value.equals("lena25"))
		  {
			  try {
					is = am.open("hamlet_lena_25.txt");
					reader = new BufferedReader(new InputStreamReader(is));
					
					while ( (line = reader.readLine()) != null )
					{
						message += line; //  + "\n";
					}
					
				} catch (IOException e) {
					e.printStackTrace();
				}
		  }
		  else if (value.equals("lena50"))
		  {
			  try {
					is = am.open("hamlet_lena_50.txt");
					reader = new BufferedReader(new InputStreamReader(is));
					
					while ( (line = reader.readLine()) != null )
					{
						message += line; //  + "\n";
					}
					
				} catch (IOException e) {
					e.printStackTrace();
				}
		  }
		  else if (value.equals("lena75"))
		  {
			  try {
					is = am.open("hamlet_lena_75.txt");
					reader = new BufferedReader(new InputStreamReader(is));
					
					while ( (line = reader.readLine()) != null )
					{
						message += line; //  + "\n";
					}
					
				} catch (IOException e) {
					e.printStackTrace();
				}
		  }
		  else
		  {
			  message = value;
		  }

		  }
		});

		alert.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
		  public void onClick(DialogInterface dialog, int whichButton) {
		    // Canceled.
		  }
		});

		alert.show();
		
	}
	
	
	
	
	
	// Get real path 
	public String getRealPathFromURI(Uri contentUri) {
	        String[] proj = { MediaStore.Images.Media.DATA };
	        CursorLoader loader = new CursorLoader(this, contentUri, proj, null, null, null);
	        Cursor cursor = loader.loadInBackground();
	        int column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
	        cursor.moveToFirst();
	        return cursor.getString(column_index);
	    }
	
}
