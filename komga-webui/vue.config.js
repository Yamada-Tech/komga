// vue.config.js
module.exports = {
  publicPath: process.env.NODE_ENV === 'production' ? './' : '/',

  pluginOptions: {
    i18n: {
      locale: 'en',
      fallbackLocale: 'en',
      localeDir: 'locales',
      enableInSFC: false,
    },
  },

  devServer: {
    allowedHosts: 'all',
    client: {
      webSocketURL: 'ws://0.0.0.0:8081/ws',
    },
  },

  chainWebpack: (config) => {
    if (config.plugins.has('fork-ts-checker')) {
      config.plugin('fork-ts-checker').tap((args) => {
        if (args && args[0]) {
          args[0].typescript = args[0].typescript || {};
          args[0].typescript.memoryLimit = 8192;
        }
        return args;
      });
    }
  },

  configureWebpack: {
    module: {
      rules: [
        {
          test: [
            /readium\/.*\.css.resource$/,
            /r2d2bc\/.*\.css.resource$/,
          ],
          type: 'asset/resource',
          generator: {
            filename: 'css/[hash].css[query]',
          },
        },
      ],
    },
  },
}
