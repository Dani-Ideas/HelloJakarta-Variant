package org.example.lib;

// Marcador comun -- no declara ningun metodo. Antes traia crear/listar/buscarPorId
// directo; ahora eso se separo en ReadService (lectura) y WriteService (escritura),
// imitando el patron del proyecto real: un Service general vacio, y las operaciones
// concretas repartidas segun si son de lectura o de escritura. Sirve como tipo comun para
// poder referirse a "cualquier Service" sin importar si es de lectura, de escritura, o
// ambos.
public interface Service<D, ID> {
}
