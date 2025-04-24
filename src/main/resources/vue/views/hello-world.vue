<template id="hello-world">
  <h1 class="hello-world">Hello, World!</h1>

  <a href="/api/files">Files</a>

  <app-frame>
    <ul class="user-overview-list">
      <li v-for="user in files">
        <a :href="`/file/${user}`">"{{ user }}"</a>
      </li>
    </ul>
  </app-frame>
</template>
<script>
app.component("hello-world", {
  template: "#hello-world",
  data: () => ({
    files: [],
  }),
  created() {
    fetch("/api/files")
        .then(response => response.json())
        .then(json => this.files = json)
        .catch(error => alert(error))
  }
});


</script>
<style>
.hello-world {
  color: goldenrod;
}
</style>