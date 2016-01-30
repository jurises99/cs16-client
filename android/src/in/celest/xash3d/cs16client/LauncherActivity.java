/*
*
*    This program is free software; you can redistribute it and/or modify it
*    under the terms of the GNU General Public License as published by the
*    Free Software Foundation; either version 2 of the License, or (at
*    your option) any later version.
*
*    This program is distributed in the hope that it will be useful, but
*    WITHOUT ANY WARRANTY; without even the implied warranty of
*    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
*    General Public License for more details.
*
*    You should have received a copy of the GNU General Public License
*    along with this program; if not, write to the Free Software Foundation,
*    Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
*
*    In addition, as a special exception, the author gives permission to
*    link the code of this program with the Half-Life Game Engine ("HL
*    Engine") and Modified Game Libraries ("MODs") developed by Valve,
*    L.L.C ("Valve").  You must obey the GNU General Public License in all
*    respects for all of the code used other than the HL Engine and MODs
*    from Valve.  If you modify this file, you may extend this exception
*    to your version of the file, but you are not obligated to do so.  If
*    you do not wish to do so, delete this exception statement from your
*    version.
*
*/
package in.celest.xash3d.cs16client;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.ComponentName;
import android.content.pm.PackageManager;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.CompoundButton;
import android.widget.ArrayAdapter;

import java.io.FileOutputStream;
import java.io.File;
import java.io.InputStream;
import java.lang.reflect.Method;

import com.google.android.gms.ads.*;

import in.celest.xash3d.cs16client.R;

public class LauncherActivity extends Activity {
	private static final int PAK_VERSION = 1;
	public final static String TAG = "LauncherActivity";
	
	static EditText cmdArgs;
	static SharedPreferences mPref;
	static Spinner mServerSpinner;
	static AdView mAdView;
	
	static Boolean isExtracting = false;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_launcher);
		mPref          = getSharedPreferences("mod", 0);
		cmdArgs        = (EditText)findViewById(R.id.cmdArgs);
		mServerSpinner = (Spinner) findViewById(R.id.serverSpinner);
		mAdView        = (AdView)  findViewById(R.id.adView);

		cmdArgs.setText(mPref.getString("argv","-dev 5 -log"));
		ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,
			R.array.avail_servers, android.R.layout.simple_spinner_item);
		adapter.setDropDownViewResource(android.R.layout.simple_spinner_item);
		mServerSpinner.setAdapter(adapter);
		mServerSpinner.setSelection(mPref.getInt("serverSpinner", 0));

		AdRequest adRequest = new AdRequest.Builder()
			.addTestDevice(AdRequest.DEVICE_ID_EMULATOR)
			.addTestDevice("B1F9AE0E2DC2387F53BE815077840D9B")
			.build();
		mAdView.loadAd(adRequest);
	}

	public void startXash(View view)
	{
		SharedPreferences.Editor editor = mPref.edit();
		String argv = cmdArgs.getText().toString();

		editor.putString("argv", argv);
		editor.putInt("serverSpinner", mServerSpinner.getSelectedItemPosition());
		editor.commit();
		editor.apply();

		switch(mServerSpinner.getSelectedItemPosition())
		{
		case 0:
			argv = argv + " -dll censored";
			break;
		case 1:
			// Engine will load libserver.so by himself
			break;
		case 2:
			String fullPath = getFilesDir().getAbsolutePath().replace("/files","/lib");
			File yapb_hardfp = new File( fullPath + "/libyapb_hardfp.so" );
			File yapb = new File( fullPath + "/libyapb.so" );
			if( yapb_hardfp.exists() && !yapb_hardfp.isDirectory() )
				argv = argv + " -dll " + yapb_hardfp.getAbsolutePath();
			else if( yapb.exists() && !yapb.isDirectory() )
				argv = argv + " -dll " + yapb.getAbsolutePath();
			else
			{
				Log.v(TAG, "YaPB not found!");
				AlertDialog.Builder notFoundDialogBuilder = new AlertDialog.Builder(this);
				notFoundDialogBuilder.setMessage(R.string.not_found_msg)
					.setTitle(R.string.not_found_title);
				notFoundDialogBuilder.create();
				return;
			}
			break;
		case 3:
		case 4:
			AlertDialog.Builder notImplementedDialogBuilder = new AlertDialog.Builder(this);
			notImplementedDialogBuilder.setMessage(R.string.not_implemented_msg)
				.setTitle(R.string.not_implemented_title);
			notImplementedDialogBuilder.create();
			return;
		}
		
		Intent intent = new Intent();
		intent.setAction("in.celest.xash3d.START");
		intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

		if(cmdArgs.length() != 0)
			intent.putExtra("argv", argv);
		intent.putExtra("gamedir", "cstrike");
		intent.putExtra("gamelibdir", getFilesDir().getAbsolutePath().replace("/files","/lib"));
		intent.putExtra("pakfile", getFilesDir().getAbsolutePath() + "/extras.pak" );
		startActivity(intent);
	}

	@Override
	public void onResume() {
	super.onResume();
	if(mAdView != null)
		mAdView.resume();
	}

	@Override
	public void onDestroy() {
	if(mAdView != null)
		mAdView.destroy();

	super.onDestroy();
	}

	@Override
	public void onPause() {
	if(mAdView != null)
		mAdView.pause();
	
	super.onPause();
	}
	
	private static int chmod(String path, int mode) {
		int ret = -1;
		try
		{
			ret = Runtime.getRuntime().exec("chmod " + Integer.toOctalString(mode) + " " + path).waitFor();
			Log.d(TAG, "chmod " + Integer.toOctalString(mode) + " " + path + ": " + ret );
		}
		catch(Exception e) 
		{
			ret = -1;
			Log.d(TAG, "chmod: Runtime not worked: " + e.toString() );
		}
		try
		{
			Class fileUtils = Class.forName("android.os.FileUtils");
			Method setPermissions = fileUtils.getMethod("setPermissions",
				String.class, int.class, int.class, int.class);
			ret = (Integer) setPermissions.invoke(null, path,
				mode, -1, -1);
		}
		catch(Exception e) 
		{
			ret = -1;
			Log.d(TAG, "chmod: FileUtils not worked: " + e.toString() );
		}
		return ret;
	}

	private static void extractFile(Context context, String path) {
		try
		{
			InputStream is = null;
			FileOutputStream os = null;
			is = context.getAssets().open(path);
			File out = new File(context.getFilesDir().getPath()+'/'+path);
			out.getParentFile().mkdirs();
			chmod( out.getParent(), 0777 );
			os = new FileOutputStream(out);
			byte[] buffer = new byte[1024];
			int length;
			while ((length = is.read(buffer)) > 0) {
				os.write(buffer, 0, length);
			}
			os.close();
			is.close();
			chmod( context.getFilesDir().getPath()+'/'+path, 0777 );
		} 
		catch( Exception e )
		{
			Log.e( TAG, "Failed to extract file:" + e.toString() );
			e.printStackTrace();
		}
			
	}
	public static void extractPAK(Context context, Boolean force) {
		if(isExtracting)
			return;
		isExtracting = true;
		try {
			if( mPref == null )
				mPref = context.getSharedPreferences("mod", 0);
		
			if( mPref.getInt( "pakversion", 0 ) == PAK_VERSION && !force )
				return;
				
			extractFile(context, "extras.pak");

			SharedPreferences.Editor editor = mPref.edit();
			editor.putInt( "pakversion", PAK_VERSION );
			editor.commit();
			editor.apply();
		} 
		catch( Exception e )
		{
			Log.e( TAG, "Failed to extract PAK:" + e.toString() );
		}
		isExtracting = false;
	}
}

