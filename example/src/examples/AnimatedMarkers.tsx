import React, {useState, useRef} from 'react';
import {View, Text, TouchableOpacity, StyleSheet, Platform} from 'react-native';
import MapView, {Marker} from 'react-native-maps';
import {AnimatedRegion} from 'react-native-maps';

const LATITUDE = 37.78825; // example value
const LONGITUDE = -122.4324; // example value
const LATITUDE_DELTA = 0.0922; // example value
const LONGITUDE_DELTA = 0.0421; // example value

// prettier-ignore
const AnimatedMarkers = ({provider}: {provider: any}) => {
  const [coordinate, setCoordinate] = useState(
    new AnimatedRegion({
      latitude: LATITUDE,
      longitude: LONGITUDE,
    })
  );

  const markerRef = useRef<any>(null);

  const animate = () => {
    const newCoordinate = {
      latitude: LATITUDE + (Math.random() - 0.5) * (LATITUDE_DELTA / 2),
      longitude: LONGITUDE + (Math.random() - 0.5) * (LONGITUDE_DELTA / 2),
    };

    if (Platform.OS === 'android') {
      if (markerRef.current) {
        markerRef.current._component.animateMarkerToCoordinate(newCoordinate, 500);
      }
    } else {
      // `useNativeDriver` defaults to false if not passed explicitly
      coordinate.timing({...newCoordinate, useNativeDriver: true}).start();
    }
  };

  return (
    <View style={styles.container}>
      <MapView
        provider={provider}
        style={styles.map}
        initialRegion={{
          latitude: LATITUDE,
          longitude: LONGITUDE,
          latitudeDelta: LATITUDE_DELTA,
          longitudeDelta: LONGITUDE_DELTA,
        }}
      >
        <Marker.Animated
          ref={markerRef}
          coordinate={coordinate}
        />
      </MapView>
      <View style={styles.buttonContainer}>
        <TouchableOpacity
          onPress={animate}
          style={[styles.bubble, styles.button]}
        >
          <Text>Animate</Text>
        </TouchableOpacity>
      </View>
    </View>
  );
};

const styles = StyleSheet.create({
  container: {
    ...StyleSheet.absoluteFillObject,
    justifyContent: 'flex-end',
    alignItems: 'center',
  },
  map: {
    ...StyleSheet.absoluteFillObject,
  },
  bubble: {
    flex: 1,
    backgroundColor: 'rgba(255,255,255,0.7)',
    paddingHorizontal: 18,
    paddingVertical: 12,
    borderRadius: 20,
  },
  latlng: {
    width: 200,
    alignItems: 'stretch',
  },
  button: {
    width: 80,
    paddingHorizontal: 12,
    alignItems: 'center',
    marginHorizontal: 10,
  },
  buttonContainer: {
    flexDirection: 'row',
    marginVertical: 20,
    backgroundColor: 'transparent',
  },
});

export default AnimatedMarkers;
