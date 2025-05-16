int main() {
  int antwort = 42;
  antwort %= 4; // 2
  antwort *= 4; // 8
  antwort = antwort * -antwort; // -64
  antwort += -antwort + antwort - antwort - antwort; // 64
  return antwort + 15 - 37;
}