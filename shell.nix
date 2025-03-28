{ pkgs ? import <nixpkgs> {} }:

pkgs.mkShell {
  name = "minecraft-factions-mod";
  buildInputs = with pkgs; [
    jdk
    jdt-language-server
  ];
  shellHook = ''
    build() {
      ./gradlew build
    }
    echo "Type 'build' to rebuild the mod jar file in build/libs."
  '';
}
