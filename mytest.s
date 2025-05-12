.global main
.global _main
.text
main:
call _main
# move the return value into the first argument for the syscall
movq %rax, %rdi
# move the exit syscall number into rax
movq $0x3C, %rax
syscall
_main:
mov %r10, 5
mov %cl, 1
shl %r10
mov %r11, 5
shl %r11
imul %r10, %r11
mov %cl, 2
shl %r10
mov %rax, %r10
ret
