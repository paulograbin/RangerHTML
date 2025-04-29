<template id="pdp">
  <app-frame>

    <div>
      <ProducCard>

      </ProducCard>
    </div>


  </app-frame>
</template>

<script setup>


app.component("pdp", {
  template: "#pdp",

  name: 'pdp',
  components: {
    ProductCard
  },


  mounted() {
    console.log('mounted');
  },

  computed() {
    console.log('computed')
  }

});
</script>


<style>

</style>