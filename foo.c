int main() {
  bool test = false;
  int result = 0xFFFA;
  if (!test) return ~result;
  return 0;
}