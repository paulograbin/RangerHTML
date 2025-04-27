<template id="hello-world">
  <h1 class="hello-world">Hello, World!</h1>

  <a href="/api/files">Files</a>

  <app-frame>
    <ul class="user-overview-list">
      <li v-for="file in files">

        <div v-if="file.tombstone">
          <p>{{ file.name }}</p>
        </div>
        <div v-else-if="!file.tombstone">
          <a :href="`/file/${file.name}`">"{{ file.name }} / {{ file.group }} - {{ file.length}}"</a>
        </div>


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
  mounted() {
    document.onreadystatechange = () => {
      document.getElementById('docSearch').addEventListener('input', function () {
        const query = this.value.toLowerCase();
        document.querySelectorAll('#documents-list .file-entry').forEach(li => {
          const filename = li.querySelector('.filename').textContent.toLowerCase();
          li.style.display = filename.includes(query) ? '' : 'none';
        });
      });
    }
  },
  created() {
    fetch("/api/files")
        .then(response => response.json())
        .then(json => this.files = json)
        .catch(error => alert(error))
  },
});
</script>
<style>
.hello-world {
  color: goldenrod;
}
</style>