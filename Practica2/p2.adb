--Autor: Constantino Byelov Serdiuk

with Ada.Text_IO; use Ada.Text_IO;
with ada.numerics.discrete_random;

procedure p2 is

    -- Declaraciones
    --Funciones para poder usar esperas random
    function randomN return Integer is
        type randRange is new Integer range 1..10;
        package Rand_Int is new ada.numerics.discrete_random(randRange);
        use Rand_Int;
        gen : Generator;
        num : randRange;
    begin
        reset(gen);
        num := random(gen);
        return Integer(num);
    end randomN;

    function randomBigN return Integer is
        type randRange is new Integer range 4..6;
        package Rand_Int is new ada.numerics.discrete_random(randRange);
        use Rand_Int;
        gen : Generator;
        num : randRange;
    begin
        reset(gen);
        num := random(gen);
        return Integer(num);
    end randomBigN;

    --Objeto protegido Puente
    package def_puente is

        --Declaraciones del objeto protegido Puente
        protected type Puente is
            procedure vehiculoLock(id: Integer; direccion: Character);
            entry cocheEntraNorte(id: Integer; direccion: Character);
            entry cocheEntraSur(id: Integer; direccion: Character);
            procedure cocheSale(id: Integer; direccion: Character);
            entry ambulanciaEntra(id: Integer);
            procedure ambulanciaSale(id: Integer);
        private
            esperaSur: Integer := 0;                --Contador de esperas en el sur
            esperaNorte: Integer := 0;              --Contador de esperas en el norte
            pasando: Boolean := false;              --Variable que controla si hay vehículos pasando por el puente o no
            ambulancia: Boolean := false;           --Variable que controla si hay una ambulancia esperando
        end Puente;
    
    end def_puente;

    package body def_puente is

        --Cuerpo del objeto protegido Puente
        protected body Puente is

            --Función para cuando los vehículos llegan al puente
            procedure vehiculoLock(id: Integer; direccion: Character) is
            begin
                --Si se trata de la ambulancia avisamos de que está esperando poniendo la variable booleana "ambulancia" a true
                if id = 112 then
                    Put_Line("+++++Ambulància " & Integer'Image(id) & " espera per entrar");
                    ambulancia := true;
                --Si se trata de un coche aumentamos el contador de espera correspondiente dependiendo de su dirección
                else
                    if direccion = 'S' then
                        esperaSur := esperaSur + 1;
                        Put_Line("El cotxe " & Integer'Image(id) & " espera a l'entrada SUD, esperen al SUD: " & Integer'Image(esperaSur));
                    else
                        esperaNorte := esperaNorte + 1;
                        Put_Line("El cotxe " & Integer'Image(id) & " espera a l'entrada NORD, esperen al NORD: " & Integer'Image(esperaNorte));
                    end if;
                end if;
            end vehiculoLock;

            --Función para que los coches que están esperando en el norte pasen el puente
            --Un coche solo podrá pasar si no hay ningún vehículo pasando actualmente, no hay una ambulancia esperando y además,
            --el número de coches esperando en el norte debe ser igual o mayor al número de coches esperando en el sur.
            --En caso de que se cumpla lo anterior ponemos la variable "pasando" a true y disminuimos el contador del norte
            entry cocheEntraNorte(id: Integer; direccion: Character) when not pasando and not ambulancia and esperaNorte >= esperaSur is
            begin
                pasando := true;
                esperaNorte := esperaNorte - 1;
                Put_Line("El cotxe " & Integer'Image(id) & " entra al pont. Esperen al NORD " & Integer'Image(esperaNorte));
            end cocheEntraNorte;

            --Función para que los coches que están esperando en el sur pasen el puente
            --Esta función es análoga a la de arriba, solo que en este caso la última condición será que el número de coches esperando
            --en el sur debe ser mayor al número de coches esperando en el norte
            entry cocheEntraSur(id: Integer; direccion: Character) when not pasando and not ambulancia and esperaSur > esperaNorte is
            begin
                pasando := true;
                esperaSur := esperaSur - 1;
                Put_Line("El cotxe " & Integer'Image(id) & " entra al pont. Esperen al SUD " & Integer'Image(esperaSur));
            end cocheEntraSur;

            --Función para que el coche salga del puente
            procedure cocheSale(id: Integer; direccion: Character) is
            begin
                Put_Line("------> El vehicle " & Integer'Image(id) & " surt del pont");
                pasando := false;
            end cocheSale;

            --Función para que la ambulancia pase por el puente
            --Aquí la única condición para que entre será que no haya ningún vehículo pasando, ya que la ambulancia tiene prioridad de paso
            entry ambulanciaEntra(id: Integer) when not pasando is
            begin
                pasando := true;
                ambulancia := false;
                Put_Line("+++++Ambulància " & Integer'Image(id) & " és al pont");
            end ambulanciaEntra;

            --Función para que la ambulancia salga del puente
            procedure ambulanciaSale(id: Integer) is
            begin
                Put_Line("------> El vehicle " & Integer'Image(id) & " surt del pont");
                pasando := false;
            end ambulanciaSale;

        end Puente;

    end def_puente;

    --Inicialización del objeto protegido puente
    miPuente: def_puente.Puente;

    --Task Vehiculo
    task type Vehiculo(id: Integer; direccion: Character);

    --Cuerpo de Vehiculo
    task body Vehiculo is
    begin

        --En caso de que se trate de la ambulancia
        if id = 112 then
            --Simulación del tiempo que tarda en ponerse en marcha
            delay Duration(randomN);
            Put_Line("L'ambulància " & Integer'Image(id) & " està en ruta");
            --Simulación del tiempo que tarda en llegar
            delay Duration(randomBigN);
            --Avisamos de que hay una ambulancia esperando
            miPuente.vehiculoLock(id, direccion);
            --Hacemos que la ambulancia pase el puente (cuando no haya ningún vehículo pasando)
            miPuente.ambulanciaEntra(id);
            --Simulación del tiempo que tarda en pasar
            delay Duration(randomBigN);
            --La ambulancia sale del puente y modifica las variables necesarias
            miPuente.ambulanciaSale(id);
        --En caso de que se trate de un coche
        else
            --Simulación del tiempo que tarda en ponerse en marcha
            delay Duration(randomN);
            Put_Line("El cotxe " & Integer'Image(id) & " està en ruta en direcció " & Character'Image(direccion));
            --Simulación del tiempo que tarda en llegar
            delay Duration(randomN);
            --El coche llega al puente y espera, aumentando el contador que correspondiente
            miPuente.vehiculoLock(id, direccion);
            --El coche entra en el puente y reduce el contador correspondiente
            if direccion = 'N' then
                miPuente.cocheEntraNorte(id, direccion);
            else
                miPuente.cocheEntraSur(id, direccion);
            end if;
            --Simulación del tiempo que tarda en pasar
            delay Duration(randomBigN);
            --El coche sale del puente y modifica las variables necesarias
            miPuente.cocheSale(id, direccion);

        end if;

    end Vehiculo;

    --Inicialización de los "procesos" Vehiculo
    coche1: Vehiculo(1,'S');
    coche2: Vehiculo(2,'N');
    coche3: Vehiculo(3,'S');
    coche4: Vehiculo(4,'N');
    coche5: Vehiculo(5,'S');
    ambulancia: Vehiculo(112, 'N');

begin

    null;

end p2;