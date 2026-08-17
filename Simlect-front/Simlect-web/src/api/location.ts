import request from './http';

export interface LocationPayload {
  latitude?: number;
  longitude?: number;
  city?: string;
  province?: string;
  district?: string;
  street?: string;
  summary?: string;
}

export type LocationWeatherPayload = LocationPayload;

export const locationApi = {
  resolve: (latitude: number, longitude: number) =>
    request.get('/location/resolve', {
      params: { latitude, longitude }
    }),

  sync: (latitude: number, longitude: number) =>
    request.get('/location/sync', {
      params: { latitude, longitude }
    })
};
