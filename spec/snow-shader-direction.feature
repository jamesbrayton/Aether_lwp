Feature: Snow Shader Particle Direction
  As a user
  I want snow particles to fall downward
  So that the snow effect appears realistic and natural

  Background:
    Given the snow shader is loaded
    And the screen resolution is 1080x1920
    And u_particleCount is 100
    And u_speed is 1.0

  Scenario: Snow particles fall downward over time
    Given u_time is 0.0
    When I sample particle positions at y-coordinate
    And u_time advances to 1.0
    And I sample the same particles again
    Then particle Y positions should have increased
    And particles should have moved downward on screen

  Scenario: Snow particles wrap from bottom to top
    Given u_time is 0.0
    When u_time advances such that fallOffset exceeds 1.0
    Then particles that reach the bottom (y=1.0)
    And should reappear at the top (y=0.0)
    And continue falling downward

  Scenario: Snow particles maintain consistent direction
    Given u_time is 0.0
    When u_time continuously increases from 0.0 to 10.0
    Then all particles should consistently move downward
    And no particles should move upward at any point
