package com.kesthers.plugins.bgs;

interface BackgroundServiceListener {
				void handleUpdate();
				String getUniqueID();
}