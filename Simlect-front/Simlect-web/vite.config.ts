import { fileURLToPath, URL } from 'node:url';
import { defineConfig, loadEnv } from 'vite';
import vue from '@vitejs/plugin-vue';
import { VitePWA } from 'vite-plugin-pwa';

export default defineConfig(({ mode }) => {
  const env = loadEnv(mode, process.cwd(), '');
  const devPort = Number(env.VITE_DEV_PORT) || 6001;
  const apiTarget = env.VITE_API_PROXY_TARGET || 'http://localhost:6050';
  const agentTarget = env.VITE_AGENT_PROXY_TARGET || 'http://localhost:7050';
  const wsTarget = env.VITE_WS_PROXY_TARGET || 'ws://localhost:7050';

  return {
    plugins: [
      vue(),
      VitePWA({
        registerType: 'autoUpdate',
        includeAssets: [
          'favicon.svg',
          'icons.svg',
          'apple-touch-icon.png',
          'pwa/apple-touch-icon.png',
          'pwa/apple-touch-icon-1024.png',
          'pwa/icon-192.png',
          'pwa/icon-512.png',
          'pwa/splash-iphone-se.png',
          'pwa/splash-iphone-12-13-14.png',
          'pwa/splash-iphone-14-pro.png',
          'pwa/splash-iphone-14-pro-max.png',
          'pwa/splash-iphone-16-pro.png',
          'pwa/splash-iphone-16-pro-max.png'
        ],
        manifest: {
          name: '简选 Simlect',
          short_name: '简选',
          description: '简选 - 精选好物，品质生活',
          theme_color: '#1d1d1f',
          background_color: '#ffffff',
          display: 'standalone',
          start_url: '/',
          icons: [
            {
              src: '/pwa/icon-192.png',
              sizes: '192x192',
              type: 'image/png',
              purpose: 'any'
            },
            {
              src: '/pwa/icon-512.png',
              sizes: '512x512',
              type: 'image/png',
              purpose: 'any'
            },
            {
              src: '/pwa/icon-512.png',
              sizes: '512x512',
              type: 'image/png',
              purpose: 'maskable'
            }
          ]
        },
        workbox: {
          globPatterns: ['**/*.{js,css,svg,png,ico,woff2,ttf}'],
          globIgnores: ['**/index.html'],
          navigateFallback: '/index.html',
          runtimeCaching: [
            {
              urlPattern: ({ request }) => request.mode === 'navigate',
              handler: 'NetworkFirst',
              options: {
                cacheName: 'html-pages',
                expiration: { maxEntries: 5, maxAgeSeconds: 60 * 60 }
              }
            },
            {
              urlPattern: /^https?:\/\/.*\.(png|jpg|jpeg|gif|webp|svg)(\?.*)?$/,
              handler: 'CacheFirst',
              options: {
                cacheName: 'image-cache',
                expiration: {
                  maxEntries: 200,
                  maxAgeSeconds: 30 * 24 * 60 * 60
                }
              }
            }
          ]
        }
      })
    ],
    resolve: {
      alias: {
        '@': fileURLToPath(new URL('./src', import.meta.url))
      }
    },
    server: {
      port: devPort,
      strictPort: true,
      host: '127.0.0.1', // 仅本机访问（防内网跳板）
      // 移除 trycloudflare 隧道放行（防内网跳板）
      proxy: {
        '/api/agent': {
          target: agentTarget,
          changeOrigin: true
        },
        '/api': {
          target: apiTarget,
          changeOrigin: true
        },
        '/ws': {
          target: wsTarget,
          ws: true,
          changeOrigin: true
        }
      }
    }
  };
});
