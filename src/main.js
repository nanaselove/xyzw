import "@arco-design/web-vue/dist/arco.css";
import "virtual:uno.css";
import "./assets/styles/global.scss";

import { createApp } from "vue";
import { createPinia } from "pinia";
import naive from "naive-ui";
import router from "./router";
import App from "./App.vue";
// import { i18n } from './locales';

// 创建应用实例
const app = createApp(App);

// 使用插件
app.use(createPinia());
app.use(router);
app.use(naive);
// app.use(i18n)

// 全局主题应用：临时固定为深色主题（禁用光暗切换）
const applyTheme = () => {
  localStorage.setItem("theme", "dark");
  document.documentElement.classList.add("dark");
  document.documentElement.setAttribute("data-theme", "dark");
  document.body.classList.add("dark");
  document.body.setAttribute("data-theme", "dark");
};

applyTheme();

// 挂载应用
app.mount("#app");
